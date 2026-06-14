package de.oumaima.servicedesk.team;


import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private OffsetDateTime createdAt;


}
