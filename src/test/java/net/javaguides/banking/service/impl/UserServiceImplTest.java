package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.UserDTO;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 單元測試")
class UserServiceImplTest {

    // @Mock: 建立 UserRepository 的模擬物件
    @Mock
    private UserRepository userRepository;

    // @Mock: 建立 RoleRepository 的模擬物件
    @Mock
    private RoleRepository roleRepository;

    // @InjectMocks: 建立 UserServiceImpl 的實例，並將上述 @Mock 物件注入其中
    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    /**
     * 在每個測試方法執行前，初始化通用的測試資料
     */
    @BeforeEach
    void setUp() {
        userRole = new Role(1, AppRole.ROLE_USER, null);
        adminRole = new Role(2, AppRole.ROLE_ADMIN, null);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUserName("testUser");
        testUser.setEmail("test@example.com");
        testUser.setRole(userRole);
        testUser.setCreatedDate(LocalDateTime.now());
        testUser.setUpdatedDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("測試 - 成功更新使用者角色")
    void testUpdateUserRole_Success() {
        // Arrange (準備)
        Long userId = 1L;
        String newRoleName = "ROLE_ADMIN";

        // 當 userRepository.findById 被呼叫時，回傳準備好的 testUser
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        // 當 roleRepository.findByRoleName 被呼叫時，回傳準備好的 adminRole
        given(roleRepository.findByRoleName(AppRole.ROLE_ADMIN)).willReturn(Optional.of(adminRole));
        // 當 userRepository.save 被呼叫時，直接回傳傳入的 User 物件
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act (執行)
        userService.updateUserRole(userId, newRoleName);

        // Assert (斷言)
        // 使用 ArgumentCaptor 捕獲傳遞給 save 方法的 User 物件
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // 驗證 userRepository.save 方法被呼叫了 1 次，並捕獲其參數
        verify(userRepository, times(1)).save(userCaptor.capture());

        // 取得被捕獲的 User 物件
        User savedUser = userCaptor.getValue();

        // 斷言被儲存的使用者角色已更新為 ADMIN
        assertEquals(AppRole.ROLE_ADMIN, savedUser.getRole().getRoleName());
    }

    @Test
    @DisplayName("測試 - 更新使用者角色失敗 (使用者不存在)")
    void testUpdateUserRole_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 99L;
        String newRoleName = "ROLE_ADMIN";
        // 模擬找不到使用者
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // Act & Assert
        // 斷言執行 updateUserRole 會拋出 RuntimeException，並檢查例外訊息
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserRole(userId, newRoleName);
        });

        assertEquals("User not found", exception.getMessage());
        // 驗證因為找不到使用者，save 方法從未被呼叫
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("測試 - 更新使用者角色失敗 (角色不存在)")
    void testUpdateUserRole_RoleNotFound_ThrowsException() {
        // Arrange
        Long userId = 1L;
        String newRoleName = "ROLE_SUPER_ADMIN"; // 一個不存在的角色
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        // 模擬找不到角色
        given(roleRepository.findByRoleName(any(AppRole.class))).willReturn(Optional.empty());

        // Act & Assert
        // 斷言執行 updateUserRole 會拋出 RuntimeException
        assertThrows(RuntimeException.class, () -> {
            userService.updateUserRole(userId, newRoleName);
        });

        // 驗證因為找不到角色，save 方法從未被呼叫
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("測試 - 成功取得所有使用者列表")
    void testGetAllUsers_ReturnsUserList() {
        // Arrange
        User anotherUser = new User("user2", "user2@example.com");
        // 模擬 findAll 回傳一個包含兩個使用者的列表
        given(userRepository.findAll()).willReturn(List.of(testUser, anotherUser));

        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertNotNull(users);
        assertEquals(2, users.size());
        // 驗證 userRepository.findAll 方法被呼叫了 1 次
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("測試 - 成功根據 ID 取得使用者並轉換為 DTO")
    void testGetUserById_Success() {
        // Arrange
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // Act
        UserDTO userDTO = userService.getUserById(userId);

        // Assert
        assertNotNull(userDTO);
        assertEquals(testUser.getUserId(), userDTO.getUserId());
        assertEquals(testUser.getUserName(), userDTO.getUserName());
        assertEquals(testUser.getEmail(), userDTO.getEmail());
        assertEquals(testUser.getRole(), userDTO.getRole());
        // 這個測試間接驗證了 private 方法 convertToDto 的正確性
    }

    @Test
    @DisplayName("測試 - 根據 ID 取得使用者失敗 (使用者不存在)")
    void testGetUserById_NotFound_ThrowsException() {
        // Arrange
        Long userId = 99L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // Act & Assert
        // 由於程式碼中未定義自訂例外，此處預期拋出 NoSuchElementException 或類似的 RuntimeException
        assertThrows(RuntimeException.class, () -> {
            userService.getUserById(userId);
        });
    }

    @Test
    @DisplayName("測試 - 成功根據使用者名稱尋找使用者")
    void testFindByUsername_Success() {
        // Arrange
        String username = "testUser";
        given(userRepository.findByUserName(username)).willReturn(Optional.of(testUser));

        // Act
        User foundUser = userService.findByUsername(username);

        // Assert
        assertNotNull(foundUser);
        assertEquals(username, foundUser.getUserName());
    }


    @Test
    @DisplayName("測試 - 根據使用者名稱尋找使用者失敗 (使用者不存在)")
    void testFindByUsername_NotFound_ThrowsException() {
        // Arrange
        String username = "nonExistentUser";
        given(userRepository.findByUserName(username)).willReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findByUsername(username);
        });

        assertEquals("User not found with username: " + username, exception.getMessage());
    }
}