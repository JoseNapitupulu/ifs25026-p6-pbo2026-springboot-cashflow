package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.Role;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.User;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class InMemoryUserRepository implements IUserRepository {
    private final InMemoryRepository<User, Integer> repository = new InMemoryRepository<>(User::getId);
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public List<User> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<User> findById(Integer id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findBy(user -> user.getUsername().equals(username)).stream().findFirst();
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public User saveNew(String username, String passwordHash, String fullName, Role role) {
        if (findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username sudah digunakan");
        }
        User user = new User(nextId.getAndIncrement(), username, passwordHash, fullName, role, null);
        return save(user);
    }

    @Override
    public boolean deleteById(Integer id) {
        return repository.deleteById(id);
    }

    @Override
    public void update(User user) {
        repository.update(user);
    }

    @Override
    public List<User> findBy(Predicate<User> predicate) {
        return repository.findBy(predicate);
    }
}
