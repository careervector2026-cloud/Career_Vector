package com.careervector.dto;

import lombok.Data;

@Data
public class StudentUpdateDto {
    private String email;

    private String mobileNumber;
    private String githubUrl;
    private String leetcodeUrl;
    private String hackerrankUrl;
    private String codechefUrl;

    private Double gpa_sem_1;
    private Double gpa_sem_2;
    private Double gpa_sem_3;
    private Double gpa_sem_4;
    private Double gpa_sem_5;
    private Double gpa_sem_6;
    private Double gpa_sem_7;
    private Double gpa_sem_8;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLeetcodeUrl() {
        return leetcodeUrl;
    }

    public void setLeetcodeUrl(String leetcodeUrl) {
        this.leetcodeUrl = leetcodeUrl;
    }

    public String getHackerrankUrl() {
        return hackerrankUrl;
    }

    public void setHackerrankUrl(String hackerrankUrl) {
        this.hackerrankUrl = hackerrankUrl;
    }

    public String getCodechefUrl() {
        return codechefUrl;
    }

    public void setCodechefUrl(String codechefUrl) {
        this.codechefUrl = codechefUrl;
    }

    public Double getGpa_sem_1() {
        return gpa_sem_1;
    }

    public void setGpa_sem_1(Double gpa_sem_1) {
        this.gpa_sem_1 = gpa_sem_1;
    }

    public Double getGpa_sem_2() {
        return gpa_sem_2;
    }

    public void setGpa_sem_2(Double gpa_sem_2) {
        this.gpa_sem_2 = gpa_sem_2;
    }

    public Double getGpa_sem_3() {
        return gpa_sem_3;
    }

    public void setGpa_sem_3(Double gpa_sem_3) {
        this.gpa_sem_3 = gpa_sem_3;
    }

    public Double getGpa_sem_4() {
        return gpa_sem_4;
    }

    public void setGpa_sem_4(Double gpa_sem_4) {
        this.gpa_sem_4 = gpa_sem_4;
    }

    public Double getGpa_sem_5() {
        return gpa_sem_5;
    }

    public void setGpa_sem_5(Double gpa_sem_5) {
        this.gpa_sem_5 = gpa_sem_5;
    }

    public Double getGpa_sem_6() {
        return gpa_sem_6;
    }

    public void setGpa_sem_6(Double gpa_sem_6) {
        this.gpa_sem_6 = gpa_sem_6;
    }

    public Double getGpa_sem_7() {
        return gpa_sem_7;
    }

    public void setGpa_sem_7(Double gpa_sem_7) {
        this.gpa_sem_7 = gpa_sem_7;
    }

    public Double getGpa_sem_8() {
        return gpa_sem_8;
    }

    public void setGpa_sem_8(Double gpa_sem_8) {
        this.gpa_sem_8 = gpa_sem_8;
    }
}
