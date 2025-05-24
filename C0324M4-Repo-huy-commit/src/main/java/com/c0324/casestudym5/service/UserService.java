package com.c0324.casestudym5.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.c0324.casestudym5.dto.ChangePasswordDTO;
import com.c0324.casestudym5.dto.UserDTO;
import com.c0324.casestudym5.model.User;

@Service
public interface UserService extends UserDetailsService {

    UserDetails loadUserByUsername(String userName);

    User findByEmail(String email);

    void save(User user);

    User getCurrentUser();

    void changePassword(ChangePasswordDTO changePasswordDTO);

    void updateProfile(UserDTO user);

    void changeAvatar(MultipartFile avatar);

    List<User> findAll();

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findAllByRole(String role);

    User findById(Long id);

    int calculateAge(User user);

}
