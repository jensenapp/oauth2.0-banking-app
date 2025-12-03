//package net.javaguides.banking.service.impl;
//
//
//import net.javaguides.banking.dto.UserDTO;
//import net.javaguides.banking.entity.AppRole;
//import net.javaguides.banking.entity.Role;
//import net.javaguides.banking.entity.User;
//import net.javaguides.banking.repository.RoleRepository;
//import net.javaguides.banking.repository.UserRepository;
//import net.javaguides.banking.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class UserServiceImpl implements UserService {
//
//    @Autowired
//    UserRepository userRepository;
//
//    @Autowired
//    RoleRepository roleRepository;
//
//    @Override
//    public void updateUserRole(Long userId, String roleName) {
//        User user = userRepository.findById(userId).orElseThrow(()
//                -> new RuntimeException("User not found"));
//        AppRole appRole = AppRole.valueOf(roleName);
//        Role role = roleRepository.findByRoleName(appRole)
//                .orElseThrow(() -> new RuntimeException("Role not found"));
//        user.setRole(role);
//        userRepository.save(user);
//    }
//
//
//    @Override
////    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }
//
//
//    @Override
//    public UserDTO getUserById(Long id) {
////        return userRepository.findById(id).orElseThrow();
//        User user = userRepository.findById(id).orElseThrow();
//        return convertToDto(user);
//    }
//
//    @Override
//    public User findByUsername(String username) {
//        User user= userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found with username: " + username));
//        return user;
//    }
//
//
//
//    private UserDTO convertToDto(User user) {
//        return new UserDTO(
//                user.getUserId(),
//                user.getUsername(),
//                user.getEmail(),
//                user.isAccountNonLocked(),
//                user.isAccountNonExpired(),
//                user.isCredentialsNonExpired(),
//                user.isEnabled(),
//                user.getCredentialsExpiryDate(),
//                user.getAccountExpiryDate(),
//                user.getTwoFactorSecret(),
//                user.isTwoFactorEnabled(),
//                user.getSignUpMethod(),
//                user.getRole(),
//                user.getCreatedDate(),
//                user.getUpdatedDate()
//        );
//    }
//
//
//}
