package com.insurance.service;

import com.insurance.dto.request.LoginRequest;
import com.insurance.dto.request.RegisterRequest;
import com.insurance.dto.response.AuthResponse;
import com.insurance.entity.User;
import com.insurance.enums.Role;
import com.insurance.exception.BusinessRuleException;
import com.insurance.repository.UserRepository;
import com.insurance.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                       AUTH SERVICE TESTS                                ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Tests the AuthService business logic in isolation using Mockito.       ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: Unit Test Principles                                    ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  F - Fast:      No DB, no network. All dependencies mocked.            ║
 * ║  I - Isolated:  Each test is independent. No shared state.             ║
 * ║  R - Repeatable: Same result every run.                                ║
 * ║  S - Self-validating: Clear pass/fail (assertions).                    ║
 * ║  T - Timely:    Written alongside production code (TDD ideally).       ║
 * ║                                                                          ║
 * ║  @ExtendWith(MockitoExtension.class): Activates Mockito's JUnit 5      ║
 * ║  integration. Automatically initializes @Mock and @InjectMocks fields. ║
 * ║                                                                          ║
 * ║  @Mock: Creates a mock (fake) implementation of the interface.         ║
 * ║  The mock records method calls and returns configured stubs.           ║
 * ║                                                                          ║
 * ║  @InjectMocks: Creates a REAL instance of the class under test,        ║
 * ║  injecting all @Mock fields via constructor/setter injection.           ║
 * ║                                                                          ║
 * ║  BDDMockito (given/when/then style):                                    ║
 * ║  given(repo.findByEmail(email)).willReturn(Optional.of(user));         ║
 * ║  → When findByEmail() is called with email, return Optional<User>      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    // ─── MOCKS (Fakes — no real implementation, we control their behavior) ─────
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    // ─── SYSTEM UNDER TEST (Real class, with mocked dependencies injected) ─────
    @InjectMocks private AuthService authService;

    // ─── COMMON TEST DATA ────────────────────────────────────────────────────────
    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User mockUser;

    /**
     * @BeforeEach: runs before EACH test method.
     * Sets up fresh test data so tests don't interfere with each other.
     */
    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .password("Password123!")
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("john.doe@email.com")
                .password("Password123!")
                .build();

        mockUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .password("$2a$10$encodedPassword")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // REGISTRATION TESTS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * @Nested: Groups related tests. Produces cleaner test output.
     * Each nested class has its own @BeforeEach if needed.
     */
    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully when email is unique")
        void shouldRegisterUserSuccessfully() {
            // ARRANGE (given): set up mock behaviors
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(mockUser);
            given(jwtService.generateToken(anyMap(), any(User.class))).willReturn("mock.jwt.token");

            // ACT (when): call the method under test
            AuthResponse response = authService.register(validRegisterRequest);

            // ASSERT (then): verify the result
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getEmail()).isEqualTo("john.doe@email.com");
            assertThat(response.getRole()).isEqualTo(Role.ROLE_CUSTOMER);
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            // VERIFY: confirm that the right methods were called on mocks
            // INTERVIEW TIP: Mockito verify() checks that a method was called
            // exactly N times with the specified arguments.
            then(userRepository).should().existsByEmail("john.doe@email.com");
            then(passwordEncoder).should().encode("Password123!");
            then(userRepository).should().save(any(User.class));
            then(jwtService).should().generateToken(anyMap(), any(User.class));
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // ARRANGE: simulate email already taken
            given(userRepository.existsByEmail("john.doe@email.com")).willReturn(true);

            // ACT + ASSERT: expect exception to be thrown
            // assertThatThrownBy: AssertJ's fluent exception testing
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already exists");

            // VERIFY: save() should NOT have been called
            then(userRepository).should(never()).save(any(User.class));
            then(jwtService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should assign CUSTOMER role to new registrations (not ADMIN/AGENT)")
        void shouldAssignCustomerRoleToNewRegistrations() {
            // ARRANGE
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encoded");
            // Capture the saved User to inspect its role
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_CUSTOMER);
                return mockUser;
            });
            given(jwtService.generateToken(anyMap(), any())).willReturn("token");

            // ACT
            authService.register(validRegisterRequest);

            // VERIFY: role was CUSTOMER (asserted inside the save() stub above)
            then(userRepository).should().save(argThat(user ->
                    user.getRole().equals(Role.ROLE_CUSTOMER)
            ));
        }

        @Test
        @DisplayName("Should hash the password before saving (never store plaintext)")
        void shouldHashPasswordBeforeSaving() {
            // ARRANGE
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode("Password123!")).willReturn("$2a$10$hashedPassword");
            given(userRepository.save(any(User.class))).willReturn(mockUser);
            given(jwtService.generateToken(anyMap(), any())).willReturn("token");

            // ACT
            authService.register(validRegisterRequest);

            // VERIFY: encode() was called with the raw password
            then(passwordEncoder).should().encode("Password123!");

            // VERIFY: the saved user does NOT have the plaintext password
            then(userRepository).should().save(argThat(user ->
                    !user.getPassword().equals("Password123!")
            ));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGIN TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // ARRANGE: mock the authentication manager returning authenticated token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
            given(authenticationManager.authenticate(any())).willReturn(authToken);
            given(jwtService.generateToken(anyMap(), any(User.class))).willReturn("mock.jwt.token");

            // ACT
            AuthResponse response = authService.login(validLoginRequest);

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getEmail()).isEqualTo("john.doe@email.com");
            assertThat(response.getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException for wrong password")
        void shouldThrowBadCredentialsForWrongPassword() {
            // ARRANGE: AuthenticationManager throws BadCredentialsException
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            // ACT + ASSERT
            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            // JWT service should never be called on failed auth
            then(jwtService).shouldHaveNoInteractions();
        }
    }
}
