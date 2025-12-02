
package net.javaguides.banking.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import net.javaguides.banking.dto.AmountRequestDto;
import net.javaguides.banking.dto.PageResponseDTO;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.enums.TransactionType;
import net.javaguides.banking.exception.InsufficientAmountException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.exception.AccountNotFoundException;
import net.javaguides.banking.security.AccountSecurityService;
import net.javaguides.banking.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 針對 AccountController 的 Web Layer 測試。
 *
 * @WebMvcTest: 只載入 Spring MVC 相關組件，專注於測試 Controller。
 * excludeAutoConfiguration = SecurityAutoConfiguration.class: 停用 Spring Security 自動配置，
 * 這將使 @PreAuthorize 等權限註解失效，讓我們能直接測試 Controller 的核心功能。
 */
@WebMvcTest(controllers = AccountController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc; // 用於模擬 HTTP 請求

    @Autowired
    private ObjectMapper objectMapper; // 用於將 Java 物件序列化為 JSON 字串

    @MockitoBean
    private AccountService accountService; // 模擬 Service 層的行為

    // --- 以下是關鍵的修正 ---
    // 即使排除了 SecurityAutoConfiguration，@WebMvcTest 仍會掃描到 @Component 註解的類別。
    // 我們需要將所有 Security 相關的 @Component 和 @Service 都模擬掉，以防止它們被實際建立。
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private AccountSecurityService accountSecurityService;

    private AccountDto accountDto;


    @BeforeEach
    void setUp() {
        // 在每個測試方法執行前，初始化一個通用的 AccountDto 物件
        accountDto = new AccountDto(1L, "testUser", new BigDecimal("1000.00"));
    }


    @Test
    @DisplayName("測試 - 新增帳戶 - 成功")
    void testAddAccount_whenValidDetailsProvided_thenReturns201Created() throws Exception {
        // Arrange
        given(accountService.createAccount(any(AccountDto.class))).willReturn(accountDto);

        String accountDtoString = objectMapper.writeValueAsString(accountDto);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(accountDtoString);

        // Act

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();

        AccountDto createdAcontent = objectMapper.readValue(contentAsString, AccountDto.class);

        // Assert

        assertEquals("testUser", createdAcontent.accountHolderName(), "姓名不一致");
        assertEquals(0, new BigDecimal(1000.00).compareTo(createdAcontent.balance()), "餘額不一致");
        assertEquals(1L, createdAcontent.id(), "id不得為空");
        assertEquals(HttpStatus.CREATED.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    @DisplayName("測試-新增帳戶-姓名為空返回400")
    void testAddAccount_whenAccountHolderNameIsEmpty_thenReturns400BadRequest() throws Exception {

        //Arrange
        accountDto = new AccountDto(2L, "", new BigDecimal("1000.00"));

        when(accountService.createAccount(any(AccountDto.class))).thenReturn(accountDto);

        String accountDtoString = objectMapper.writeValueAsString(accountDto);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(accountDtoString);

        //Act

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();


        //Assert

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    @DisplayName("測試-新增帳戶-餘額為空返回400")
    void testAddAccount_whenBalanceIsNull_thenReturns400BadRequest() throws Exception {
        //Arrange
        accountDto = new AccountDto(2L, "tommy", null);

        when(accountService.createAccount(any(AccountDto.class))).thenReturn(accountDto);

        String valueAsString = objectMapper.writeValueAsString(accountDto);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(valueAsString);

        //Act

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        //Assert

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), "拋出錯誤代碼有誤");
    }

    @Test
    @DisplayName("測試-新增帳戶-餘額為負數返回400")
    void testAddAccount_whenBalanceIsNegative_thenReturns400BadRequest() throws Exception {
        //Arrange

        accountDto = new AccountDto(2L, "tommy", new BigDecimal("-1000000000"));

        when(accountService.createAccount(any(AccountDto.class))).thenReturn(accountDto);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountDto));

        //Act
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        //Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), "拋出錯誤代碼有誤");
    }

    @Test
    @DisplayName("測試-從Id獲取帳戶-成功")
    void testGetAccountById_whenAccountIdExists_thenReturnsAccountDetails() throws Exception {
        //Arrange
        when(accountService.getAccountById(1L)).thenReturn(accountDto);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/accounts/{id}", 1L)
                .accept(MediaType.APPLICATION_JSON);

        //Act

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        AccountDto readValue = objectMapper.readValue(contentAsString, AccountDto.class);

        //Assert
        assertNotNull(readValue);
        assertEquals("testUser", readValue.accountHolderName(), "使用者姓名不一致");
        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus(), "回應碼必須是200");
    }

    @Test
    @DisplayName("測試-從Id獲取帳戶-Id不存在返回404")
    void testGetAccountById_whenAccountIdDoesNotExist_thenReturns404NotFound() throws Exception {
        //Arrange
        when(accountService.getAccountById(99L)).thenThrow(new AccountNotFoundException("Account does not exist"));

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/accounts/{id}", 99L)
                .accept(MediaType.APPLICATION_JSON);

        //Act
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), mvcResult.getResponse().getStatus(), "Id不存在返回404");
    }

    @Test
    @DisplayName("測試-查詢全部帳號-成功返回帳號分頁")
    void testGetAllAccounts_whenAccountsExist_thenReturnsPageOfAccounts() throws Exception {

        // Arrange

        AccountDto accountDto1 = new AccountDto(1L, "aaaaaa", new BigDecimal("1000000.00"));
        AccountDto accountDto2 = new AccountDto(2L, "bbbbbb", new BigDecimal("1000000.00"));

        List<AccountDto> mockedDtoList = List.of(accountDto1, accountDto2);

        Pageable pageable = PageRequest.of(0, 3, Sort.by("id").descending());

        PageImpl<AccountDto> accountDtos = new PageImpl<AccountDto>(mockedDtoList, pageable, mockedDtoList.size());

        when(accountService.getAllAccounts(any(Pageable.class))).thenReturn(accountDtos);

        RequestBuilder requestBuilder = get("/api/accounts")
                .accept(MediaType.APPLICATION_JSON)
                .param("pageNo", "0")
                .param("pageSize", "3")
                .param("sortBy", "id")
                .param("sortDir", "desc");

        // Act

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();

        TypeReference<PageResponseDTO<AccountDto>> typeReference = new TypeReference<>() {
        };

        PageResponseDTO<AccountDto> pageResponseDTO = objectMapper.readValue(contentAsString, typeReference);


        // Assert
        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus(), "回應代碼有誤");
        assertEquals(2, pageResponseDTO.getContent().size(), "回傳的帳號數量不一致");
        assertEquals(0, pageResponseDTO.getPageNo(), "回傳pageNo錯誤");
        assertEquals(3, pageResponseDTO.getPageSize(), "回傳pageSize錯誤");
        assertEquals("aaaaaa", pageResponseDTO.getContent().get(0).accountHolderName(), "姓名不一致");

    }

    @Test
    @DisplayName("測試-查詢全部帳號-無資料返回空頁面")
    void testGetAllAccounts_whenNoAccounts_thenReturnsEmptyPage() throws Exception {
        //Arrange

        Pageable pageable = PageRequest.of(0, 3, Sort.by("id").descending());
        PageImpl<AccountDto> accountDtos = new PageImpl<AccountDto>(List.of(), pageable, 0);

        when(accountService.getAllAccounts(any(Pageable.class))).thenReturn(accountDtos);

        //Act//Assert

        mockMvc.perform(get("/api/accounts")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("pageNo", "0")
                        .param("pageSize", "3")
                        .param("sortBy", "id")
                        .param("sortDir", "desc")
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("測試-獲取交易紀錄成功")
    void testFetchAccountTransactions_whenAccountIdExists_thenReturnsPageOfTransactions() throws Exception {
        //Arrange

        TransactionDTO transactionDTO1 = new TransactionDTO(1L, 1L, new BigDecimal(1000.00), TransactionType.DEPOSIT, LocalDateTime.now());
        TransactionDTO transactionDTO2 = new TransactionDTO(2L, 2L, new BigDecimal(1000.00), TransactionType.DEPOSIT, LocalDateTime.now());

        List<TransactionDTO> transactionDTOList = List.of(transactionDTO1, transactionDTO2);

        Pageable pageable = PageRequest.of(0, 3);

        PageImpl<TransactionDTO> transactionDTOS = new PageImpl<TransactionDTO>(transactionDTOList, pageable, 2);

        when(accountService.getAccountTransactions(any(Long.class), any(Pageable.class))).thenReturn(transactionDTOS);

        //Act//Assert

        mockMvc.perform(get("/api/accounts/{id}/transactions", 2L)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("pageNo", "0")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(2)))
                .andExpect(jsonPath("$.pageNo", is(0)))
                .andExpect(jsonPath("$.pageSize", is(3)));

    }

    @Test
    @DisplayName("測試-提款成功")
    void testWithdraw_whenSufficientFunds_thenReturnsUpdatedAccount() throws Exception {
        //Arrange

        when(accountService.withdraw(any(Long.class), any(BigDecimal.class))).thenReturn(accountDto);

        AmountRequestDto amountRequestDto = new AmountRequestDto(new BigDecimal("1000.00"));


        //Act//Assert
        mockMvc.perform(put("/api/accounts/{id}/withdraw", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(amountRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(accountDto.id().intValue())))
                .andExpect(jsonPath("$.accountHolderName", equalTo(accountDto.accountHolderName())));

    }

    @Test
    @DisplayName("測試-提款餘額不足-返回400BadRequest")
    void testWithdraw_whenInsufficientFunds_thenReturns400BadRequest() throws Exception {

        //Arrange

        AmountRequestDto amountRequestDto = new AmountRequestDto(new BigDecimal("10000.00"));

        when(accountService.withdraw(any(Long.class), any(BigDecimal.class))).thenThrow(new InsufficientAmountException("Insufficient amount"));


        RequestBuilder requestBuilder = put("/api/accounts/{id}/withdraw", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(amountRequestDto));

        //Act //Assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("測試-提款-AccountId不存在返回404NotFound")
    void testWithdraw_whenAccountIdDoesNotExist_thenReturns404NotFound() throws Exception {
        //Arrange
        AmountRequestDto amountRequestDto =
                new AmountRequestDto(new BigDecimal("1000.00"));

        when(accountService.withdraw(any(Long.class),any(BigDecimal.class))).
                thenThrow(new AccountNotFoundException("Account does not exist"));

        RequestBuilder requestBuilder = put("/api/accounts/{id}/withdraw", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(amountRequestDto));

        //Act

        //Assert

        mockMvc
                .perform(requestBuilder)
                .andExpect(status().isNotFound());

    }


}