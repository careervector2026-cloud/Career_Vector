package com.careervector.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name="recruiter")
public class Recruiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Setter
    @Column(nullable = false)
    private String fullName;

    @Setter
    @Getter
    @Column(unique = true, nullable = false)
    private String email;

    @Setter
    @Getter
    @Column(unique = true)
    private String userName;

    @Setter
    private String mobile;
    @Getter
    @Setter
    private String companyName;
    @Setter
    private String role;
    @Setter
    @Getter
    private String password;
    @Setter
    @Getter
    private String imageUrl;

    // New field to track verification status
    @Setter
    @Getter
    private boolean verified = false;

    // --- ADD GETTER AND SETTER ---
    @Setter
    @Getter
    @OneToMany(mappedBy = "recruiter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // 👈 ADD THIS LINE
    private List<Job> jobs;


    public String getFullName() {
        return fullName;
    }

    public String getMobile() {
        return mobile;
    }

    public String getRole() {
        return role;
    }

}
