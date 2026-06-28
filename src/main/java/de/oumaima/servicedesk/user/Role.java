package de.oumaima.servicedesk.user;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public Role() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
