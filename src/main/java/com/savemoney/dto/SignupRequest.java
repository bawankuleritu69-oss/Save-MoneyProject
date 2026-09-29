package com.savemoney.dto;

public class SignupRequest {
    private String fullName;
    private String email;
    private String password;
    private String about;

    public SignupRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
}