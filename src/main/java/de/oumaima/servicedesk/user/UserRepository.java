package de.oumaima.servicedesk.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("select u from User u left join fetch u.roles left join fetch u.team where u.email = :email")
    Optional<User> findByEmailWithRolesAndTeam(@Param("email") String email);
}
