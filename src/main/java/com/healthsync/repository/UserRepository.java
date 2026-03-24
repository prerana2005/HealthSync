package com.healthsync.repository;

import com.healthsync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndPasswordHash(String email, String passwordHash);
    List<User> findByRoleType(User.RoleType roleType);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(u.userId, 4) AS int)), 0) FROM User u")
    int findMaxUserId();
}
