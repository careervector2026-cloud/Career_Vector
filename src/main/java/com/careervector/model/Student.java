package com.careervector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Student {

    @Id
    private String rollNumber;
    private String fullName;
    @Column(unique = true)
    private String email;
    private String password;
    private String userName;
    private String dept;
    private String branch;
    @Column(length = 15) // Optional: limits length in DB
    private String mobileNumber;
    @JsonProperty("Semester")
    private int semester;
    private int year;
    @Column(columnDefinition = "TEXT")
    @Getter
    @Setter
    private String skills;
    @JsonProperty("gpa_sem_1") private Double gpaSem1;
    @JsonProperty("gpa_sem_2") private Double gpaSem2;
    @JsonProperty("gpa_sem_3") private Double gpaSem3;
    @JsonProperty("gpa_sem_4") private Double gpaSem4;
    @JsonProperty("gpa_sem_5") private Double gpaSem5;
    @JsonProperty("gpa_sem_6") private Double gpaSem6;
    @JsonProperty("gpa_sem_7") private Double gpaSem7;
    @JsonProperty("gpa_sem_8") private Double gpaSem8;
    private String profileImageUrl;
    private String resumeUrl;
    private String leetcodeurl;
    private String githubUrl;
    private boolean verified;

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Double getGpaSem1() {
        return gpaSem1;
    }

    public void setGpaSem1(Double gpaSem1) {
        this.gpaSem1 = gpaSem1;
    }

    public Double getGpaSem2() {
        return gpaSem2;
    }

    public void setGpaSem2(Double gpaSem2) {
        this.gpaSem2 = gpaSem2;
    }

    public Double getGpaSem3() {
        return gpaSem3;
    }

    public void setGpaSem3(Double gpaSem3) {
        this.gpaSem3 = gpaSem3;
    }

    public Double getGpaSem4() {
        return gpaSem4;
    }

    public void setGpaSem4(Double gpaSem4) {
        this.gpaSem4 = gpaSem4;
    }

    public Double getGpaSem5() {
        return gpaSem5;
    }

    public void setGpaSem5(Double gpaSem5) {
        this.gpaSem5 = gpaSem5;
    }

    public Double getGpaSem6() {
        return gpaSem6;
    }

    public void setGpaSem6(Double gpaSem6) {
        this.gpaSem6 = gpaSem6;
    }

    public Double getGpaSem7() {
        return gpaSem7;
    }

    public void setGpaSem7(Double gpaSem7) {
        this.gpaSem7 = gpaSem7;
    }

    public Double getGpaSem8() {
        return gpaSem8;
    }

    public void setGpaSem8(Double gpaSem8) {
        this.gpaSem8 = gpaSem8;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getLeetcodeurl() {
        return leetcodeurl;
    }

    public void setLeetcodeurl(String leetcodeurl) {
        this.leetcodeurl = leetcodeurl;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}