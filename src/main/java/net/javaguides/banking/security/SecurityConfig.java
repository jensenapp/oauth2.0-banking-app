package net.javaguides.banking.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 的主要設定檔。
 *
 * @Configuration 標示這是一個 Spring 的設定類別，Spring 容器會掃描並處理其中的 Bean。
 */
@Configuration
/**
 * @EnableMethodSecurity 啟用方法層級的安全性控制。
 * 這允許我們在個別的 Controller 方法上使用 @PreAuthorize, @PostAuthorize, @Secured 等註解來進行更細粒度的權限控制。
 * - prePostEnabled = true: 啟用 @PreAuthorize 和 @PostAuthorize 註解。
 * - securedEnabled = true: 啟用 @Secured 註解。
 * - jsr250Enabled = true: 啟用 JSR-250 標準的 @RolesAllowed 註解。
 */
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
public class SecurityConfig {




    /**
     * 定義一個 SecurityFilterChain Bean，這是 Spring Security 6.x 之後的核心設定方式。
     * 它定義了 HTTP 請求的安全處理規則鏈。
     *
     * @param http HttpSecurity 物件，用來建構安全規則。
     * @return 一個建構好的 SecurityFilterChain 實例。
     * @throws Exception 可能拋出的例外。
     */
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

        // --- 1. 設定 CSRF (跨站請求偽造) 保護 ---
       http.csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable());

        // --- 2. 設定 HTTP 請求的授權規則 ---
        http.authorizeHttpRequests((requests) -> requests
                // 規則 2.1: 任何對 "/api/csrf-token" 路徑的請求，都允許存取 (permitAll)。
                // 這讓未登入的使用者也能獲取 CSRF Token。
                .requestMatchers("/api/csrf-token").permitAll()

                // 規則 2.2 (兜底規則): 除了上述規則之外的任何其他請求 (anyRequest)，都必須經過身份驗證 (authenticated)。
                .anyRequest().authenticated()
        );

        // --- 3. 設定認證方式 ---
        // 啟用 HTTP Basic Authentication。
        // 這會彈出一個瀏覽器內建的簡單登入視窗，要求輸入使用者名稱和密碼。
//        http.httpBasic(withDefaults());
        // 啟用表單登入 (Form Login)。
        // 這會提供一個預設的登入頁面。
//        http.formLogin(withDefaults());


// 將 Session 管理設為無狀態
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 3. 設定為 OAuth2 Resource Server (JWT 模式)
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        );

        // --- 4. 建構並返回 SecurityFilterChain 物件 ---
        return http.build();
    }


}