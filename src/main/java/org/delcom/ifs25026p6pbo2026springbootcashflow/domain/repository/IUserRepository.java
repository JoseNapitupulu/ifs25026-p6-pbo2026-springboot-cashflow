package org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;

import java.util.Optional;

public interface IUserRepository extends IRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    User saveNew(String username, String passwordHash, String fullName, Role role);
}
