package org.example.technihongo.repositories;

import org.example.technihongo.entities.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    User findUserByEmail(String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    User findByUserId(Integer userId);
    List<User> findByRole_RoleId(Integer roleId);

    Page<User> findByRole_RoleId(Integer roleId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role.roleId = 3 AND (u.userName LIKE %:keyword% OR u.email LIKE %:keyword%)")
    Page<User> findStudentsByKeyword(String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role.roleId = 2 AND (u.userName LIKE %:keyword% OR u.email LIKE %:keyword%)")
    Page<User> findContentManagersByKeyword(String keyword, Pageable pageable);

    User findByStudentStudentId(Integer studentId);
}
