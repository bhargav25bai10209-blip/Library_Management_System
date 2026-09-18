package com.library.service;

import com.library.exception.UserNotFoundException;
import com.library.model.Member;
import com.library.model.Role;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.util.InputValidator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing member profiles, account status, and patron queries.
 */
public class MemberService {

    private final UserRepository userRepository;

    public MemberService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Member getMemberById(String userId) throws UserNotFoundException {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (!(u instanceof Member member)) {
            throw new IllegalArgumentException("User " + userId + " is a staff member, not a library patron.");
        }
        return member;
    }

    public List<Member> getAllMembers() {
        return userRepository.findAll().stream()
                .filter(u -> u instanceof Member)
                .map(u -> (Member) u)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public synchronized void updateProfile(String userId, String name, String phone) throws UserNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (InputValidator.isNotEmpty(name)) {
            user.setName(name.trim());
        }
        if (InputValidator.isValidPhone(phone)) {
            user.setPhone(phone.trim());
        }
        userRepository.save(user);
    }

    public synchronized void toggleAccountStatus(String userId) throws UserNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
}
