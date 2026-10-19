const express = require('express');
const session = require('express-session');
const bcrypt = require('bcrypt');
const sqlite3 = require('sqlite3').verbose();
const bodyParser = require('body-parser');
const path = require('path');

const app = express();
const PORT = 3000;

// Middleware
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(express.static(__dirname)); // Serve HTML files

// Session configuration
app.use(session({
  secret: 'savemoney-secret-key-change-in-production',
  resave: false,
  saveUninitialized: false,
  cookie: { 
    secure: false,   // set to true if using HTTPS
    httpOnly: true,
    maxAge: 1000 * 60 * 60 * 24 // 24 hours
  }
}));

// Initialize SQLite database
const db = new sqlite3.Database('./database.sqlite', (err) => {
  if (err) console.error('Database error:', err.message);
  else console.log('Connected to SQLite database');
});

// Create tables
db.serialize(() => {
  // Users table
  db.run(`CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fullName TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    about TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
  )`);

  // Expenses table
  db.run(`CREATE TABLE IF NOT EXISTS expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId INTEGER NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    amount REAL NOT NULL,
    date TEXT NOT NULL,
    time TEXT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES users (id) ON DELETE CASCADE
  )`);
});

// ---------- AUTHENTICATION MIDDLEWARE ----------
function isAuthenticated(req, res, next) {
  if (req.session.userId) return next();
  res.status(401).json({ success: false, message: 'Unauthorized' });
}

// ---------- AUTH ROUTES ----------
// Register
app.post('/api/auth/register', async (req, res) => {
  const { fullName, email, password, about } = req.body;
  
  if (!fullName || !email || !password) {
    return res.status(400).json({ success: false, message: 'Missing required fields' });
  }

  try {
    // Check if user exists
    const existing = await new Promise((resolve, reject) => {
      db.get('SELECT id FROM users WHERE email = ?', [email], (err, row) => {
        if (err) reject(err);
        resolve(row);
      });
    });

    if (existing) {
      return res.status(400).json({ success: false, message: 'Email already registered' });
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);
    
    // Insert user
    await new Promise((resolve, reject) => {
      db.run(
        'INSERT INTO users (fullName, email, password, about) VALUES (?, ?, ?, ?)',
        [fullName, email, hashedPassword, about || ''],
        function(err) {
          if (err) reject(err);
          resolve(this.lastID);
        }
      );
    });

    res.json({ success: true, message: 'Registration successful' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// Login
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ success: false, message: 'Email and password required' });
  }

  try {
    const user = await new Promise((resolve, reject) => {
      db.get('SELECT * FROM users WHERE email = ?', [email], (err, row) => {
        if (err) reject(err);
        resolve(row);
      });
    });

    if (!user) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    const match = await bcrypt.compare(password, user.password);
    if (!match) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    // Set session
    req.session.userId = user.id;
    req.session.userEmail = user.email;
    
    res.json({ success: true, message: 'Login successful' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

// Get current user (for dashboard auth check)
app.get('/api/auth/current-user', isAuthenticated, (req, res) => {
  res.json({ 
    loggedIn: true, 
    user: { 
      id: req.session.userId, 
      email: req.session.userEmail 
    } 
  });
});

// Logout
app.post('/api/auth/logout', (req, res) => {
  req.session.destroy((err) => {
    if (err) return res.status(500).json({ success: false });
    res.clearCookie('connect.sid');
    res.json({ success: true });
  });
});

// ---------- EXPENSE ROUTES ----------
// Get all expenses for logged-in user
app.get('/api/expenses', isAuthenticated, (req, res) => {
  db.all(
    'SELECT * FROM expenses WHERE userId = ? ORDER BY date DESC, time DESC',
    [req.session.userId],
    (err, rows) => {
      if (err) {
        console.error(err);
        return res.status(500).json([]);
      }
      res.json(rows);
    }
  );
});

// Add new expense
app.post('/api/expenses', isAuthenticated, (req, res) => {
  const { category, description, amount, date, time } = req.body;
  
  if (!category || !description || !amount || !date) {
    return res.status(400).json({ success: false, message: 'Missing expense fields' });
  }

  db.run(
    'INSERT INTO expenses (userId, category, description, amount, date, time) VALUES (?, ?, ?, ?, ?, ?)',
    [req.session.userId, category, description, amount, date, time || '12:00'],
    function(err) {
      if (err) {
        console.error(err);
        return res.status(500).json({ success: false });
      }
      res.json({ success: true, id: this.lastID });
    }
  );
});

// Update expense
app.put('/api/expenses/:id', isAuthenticated, (req, res) => {
  const expenseId = req.params.id;
  const { category, description, amount, date, time } = req.body;

  // Verify expense belongs to user
  db.get(
    'SELECT id FROM expenses WHERE id = ? AND userId = ?',
    [expenseId, req.session.userId],
    (err, row) => {
      if (err || !row) {
        return res.status(404).json({ success: false, message: 'Expense not found' });
      }

      db.run(
        'UPDATE expenses SET category = ?, description = ?, amount = ?, date = ?, time = ? WHERE id = ?',
        [category, description, amount, date, time, expenseId],
        function(err) {
          if (err) {
            console.error(err);
            return res.status(500).json({ success: false });
          }
          res.json({ success: true });
        }
      );
    }
  );
});

// Delete expense
app.delete('/api/expenses/:id', isAuthenticated, (req, res) => {
  const expenseId = req.params.id;

  db.run(
    'DELETE FROM expenses WHERE id = ? AND userId = ?',
    [expenseId, req.session.userId],
    function(err) {
      if (err) {
        console.error(err);
        return res.status(500).json({ success: false });
      }
      res.json({ success: true });
    }
  );
});

// Get statistics (totals and category summary)
app.get('/api/expenses/stats', isAuthenticated, (req, res) => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const today = now.toISOString().split('T')[0];
  
  // Get start of week (Monday)
  const dayOfWeek = now.getDay();
  const startOfWeek = new Date(now);
  startOfWeek.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
  const weekStart = startOfWeek.toISOString().split('T')[0];
  const weekEnd = today;

  db.all(
    `SELECT category, amount, date FROM expenses 
     WHERE userId = ? AND date >= ? AND date <= ?`,
    [req.session.userId, `${year}-${month}-01`, `${year}-${month}-31`],
    (err, monthExpenses) => {
      if (err) return res.status(500).json({});
      
      const monthTotal = monthExpenses.reduce((sum, e) => sum + e.amount, 0);
      
      // Today's total
      const todayTotal = monthExpenses.filter(e => e.date === today).reduce((sum, e) => sum + e.amount, 0);
      
      // This week total
      const weekTotal = monthExpenses.filter(e => e.date >= weekStart && e.date <= weekEnd).reduce((sum, e) => sum + e.amount, 0);
      
      // Category summary for current month
      const categorySummary = {};
      monthExpenses.forEach(e => {
        categorySummary[e.category] = (categorySummary[e.category] || 0) + e.amount;
      });
      
      res.json({
        monthTotal,
        todayTotal,
        weekTotal,
        categorySummary
      });
    }
  );
});

// Serve the main page (optional redirect)
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'home.html'));
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
  console.log('Open http://localhost:3000/home.html in your browser');
});