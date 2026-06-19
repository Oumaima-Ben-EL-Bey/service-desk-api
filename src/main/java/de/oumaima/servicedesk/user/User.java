package de.oumaima.servicedesk.user;


import de.oumaima.servicedesk.team.Team;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 150)
    private String fullName;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
