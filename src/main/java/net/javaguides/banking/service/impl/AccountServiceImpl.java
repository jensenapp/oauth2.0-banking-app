package net.javaguides.banking.service.impl;

import net.javaguides.banking.dto.AccountDto;
import net.javaguides.banking.dto.TransactionDTO;
import net.javaguides.banking.dto.TransferFundDTO;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.entity.Transaction;
import net.javaguides.banking.entity.User;
import net.javaguides.banking.enums.TransactionType;
import net.javaguides.banking.exception.AccountException;
import net.javaguides.banking.exception.AccountNotFoundException;
import net.javaguides.banking.exception.InsufficientAmountException;
import net.javaguides.banking.mapper.AccountMapper;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.repository.TransactionRepository;
import net.javaguides.banking.repository.UserRepository;
import net.javaguides.banking.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Transactional
@Service
public class AccountServiceImpl implements AccountService {

    private AccountRepository accountRepository;

    private TransactionRepository transactionRepository;

    private UserRepository userRepository;

    private AccountMapper accountMapper;

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

//    private static final String TRANSACTION_TYPE_DEPOSIT = "deposit";
//    private static final String TRANSACTION_TYPE_WITHDRAW = "withdraw";
//    private static final String TRANSACTION_TYPE_TRANSACTION = "transaction";


    public AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository, UserRepository userRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public AccountDto createAccount(AccountDto accountDto) {


        Authentication auth = SecurityContextHolder.getContext().getAuthentication();


        Jwt jwt=(Jwt) auth.getPrincipal();

        String username = jwt.getClaimAsString("preferred_username");


        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Logged-in user not found in database with id"));


        Account account = accountMapper.mapTOAccount(accountDto);

        account.setAccountHolderName(user.getRealName());

        account.setUser(user);

        Account saveAccount = accountRepository.save(account);

        logger.info("成功啟用新帳戶,id為{}", saveAccount.getId());
        AccountDto accountDto1 = accountMapper.mapTOAccountDto(saveAccount);
        return accountDto1;
    }

    @Transactional(readOnly = true)
    @Override
    public AccountDto getAccountById(Long id) {
        logger.info("使用ID：{}查詢帳戶", id);
        Account account = accountRepository.findById(id).orElseThrow(() ->
        {
            logger.error("查無ID:{}", id);
            return new AccountNotFoundException("Account does not exist");
        });
        logger.info("成功取得帳號:{}", id);
        return accountMapper.mapTOAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, BigDecimal amount) {

        final int MAX_ATTEMPS = 3;

        for (int attemp = 0; attemp < MAX_ATTEMPS; attemp++) {

            try {
                logger.info("嘗試儲蓄{}進入帳號:{}", amount, id);
                Account account = accountRepository.
                        findById(id).orElseThrow(() -> {
                            logger.error("儲蓄失敗,查無ID:{}", id);
                            return new AccountNotFoundException("Account does not exist");
                        });

                account.setBalance(account.getBalance().add(amount));

                Account saveAccount = accountRepository.save(account);
                logger.info("儲蓄成功,帳號:{},新餘額:{}", id, saveAccount.getBalance());


                // 記錄交易
                Transaction transaction = new Transaction();
                transaction.setAccountId(id);
                transaction.setAmount(amount);
                transaction.setTimestamp(LocalDateTime.now());
                transaction.setTransactionType(TransactionType.DEPOSIT);
                transactionRepository.save(transaction);

                AccountDto accountDto = accountMapper.mapTOAccountDto(saveAccount);

                return accountDto;

            } catch (ObjectOptimisticLockingFailureException e) {
                // 發生衝突，記錄日誌後，迴圈將自動重試
                logger.warn("帳戶 {} 存款發生併發衝突，準備重試...", id);
            }
        }
        // 如果重試全部失敗，則拋出例外
        throw new AccountException("存款操作因高併發衝突而失敗，請稍後再試。");
    }


    @Override
    public AccountDto withdraw(Long id, BigDecimal amount) {

        final int MAX_ATTEMP=3;

        for (int attemp = 0; attemp < MAX_ATTEMP; attemp++) {


            try {
                logger.info("嘗試取款:{},扣款帳號:{}", id, amount);
                Account account = accountRepository.findById(id).orElseThrow(() -> {
                    logger.error("取款失敗,查無帳號{}");
                    return new AccountNotFoundException("Account does not exist");
                });

                if (account.getBalance().compareTo(amount) < 0) {
                    logger.error("帳號{}餘額不足,取款失敗,帳戶餘額:{},取款金額{}", id, account.getBalance(), account);
                    throw new InsufficientAmountException("Insufficient amount");
                }


                account.setBalance(account.getBalance().subtract(amount));
                accountRepository.save(account);
                logger.info("帳號{}取款成功，新餘額｛｝", account.getBalance());


                // 記錄交易
                Transaction transaction = new Transaction();
                transaction.setAccountId(id);
                transaction.setAmount(amount);
                transaction.setTimestamp(LocalDateTime.now());
                transaction.setTransactionType(TransactionType.WITHDRAW);

                transactionRepository.save(transaction);


                AccountDto accountDto = accountMapper.mapTOAccountDto(account);

                return accountDto;
            } catch (ObjectOptimisticLockingFailureException e) {
                logger.warn("帳戶{} 存款發生併發衝突，準備重試...", id);
            }
        }
        throw new AccountException("存款操作因高併發衝突而失敗，請稍後再試。");
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AccountDto> getAllAccounts(Pageable pageable) {

        Page<Account> accounts = accountRepository.findAll(pageable);

        Page<AccountDto> accountDtoPage = accounts.map(account -> accountMapper.mapTOAccountDto(account));

        return accountDtoPage;
    }

    @Override
    public void deleteAccount(Long id) {
        logger.info("嘗試刪除帳戶,帳號:{}", id);
        Account account = accountRepository.findById(id).orElseThrow(() -> {
            logger.error("刪除失敗,查無帳號:{}", id);
            return new AccountNotFoundException("Account does not exist");
        });
        accountRepository.deleteById(id);
        logger.info("刪除成功,帳號{}", id);
    }

    @Override
    public void transferFunds(TransferFundDTO transferFundDTO) {
        logger.info("從帳號{}向帳號{},發起金額為{}的轉帳", transferFundDTO.fromAccountId(), transferFundDTO.toAccountId(), transferFundDTO.amount());
        Long fromAccountId = transferFundDTO.fromAccountId();
        Long toAccountId = transferFundDTO.toAccountId();


        if (fromAccountId.equals(toAccountId)) {
            logger.error("轉帳失敗,不能轉帳給相同的帳號{}", fromAccountId);
            throw new AccountException("不能轉帳到相同帳戶");
        }

        Account account1, account2;

        if (fromAccountId < toAccountId) {
            account1 = accountRepository.findByIdForUpdate(fromAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
            account2 = accountRepository.findByIdForUpdate(toAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
        } else {
            account2 = accountRepository.findByIdForUpdate(toAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
            account1 = accountRepository.findByIdForUpdate(fromAccountId).orElseThrow(() -> new AccountNotFoundException("Account does not exist"));
        }
        // 找出哪個是轉出帳戶，哪個是轉入帳戶

        Account fromAccount = account1.getId().equals(fromAccountId) ? account1 : account2;
        Account toAccount = account2.getId().equals(toAccountId) ? account2 : account1;


//        // 1. 檢索轉出帳戶
//        Account fromAccount = accountRepository.findById(transferFundDTO.fromAccountId()).orElseThrow(() -> new AccountException("Account does not exist"));
//        // 2. 檢索轉入帳戶
//         Account toAccount = accountRepository.findById(transferFundDTO.toAccountId()).orElseThrow(() -> new AccountException("Account does not exist"));


        if (fromAccount.getBalance().compareTo(transferFundDTO.amount()) < 0) {
            logger.error("轉帳失敗,帳戶{}餘額{}小於欲轉金額{}", fromAccountId, fromAccount.getBalance(), transferFundDTO.amount());
            throw new InsufficientAmountException("Insufficient amount");
        }

        // 3. 從轉出帳戶扣款
        fromAccount.setBalance(fromAccount.getBalance().subtract(transferFundDTO.amount()));
        // 4. 轉入帳戶存入金額
        toAccount.setBalance(toAccount.getBalance().add(transferFundDTO.amount()));
        // 5. 儲存更新
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 記錄轉出方交易（TRANSFER_OUT）
        Transaction fromTransaction = new Transaction();

        fromTransaction.setAccountId(transferFundDTO.fromAccountId());
        fromTransaction.setAmount(transferFundDTO.amount());
        fromTransaction.setTimestamp(LocalDateTime.now());
        fromTransaction.setTransactionType(TransactionType.TRANSFER_OUT);
        transactionRepository.save(fromTransaction);

        // 記錄轉入方交易（TRANSFER_IN）
        Transaction toTransaction = new Transaction();

        toTransaction.setAccountId(transferFundDTO.toAccountId());
        toTransaction.setAmount(transferFundDTO.amount());
        toTransaction.setTimestamp(LocalDateTime.now());
        toTransaction.setTransactionType(TransactionType.TRANSFER_IN);
        transactionRepository.save(toTransaction);
        logger.info("資金從帳戶 {} 轉至帳戶 {} 已成功完成", fromAccountId, toAccountId);

    }


    @Transactional(readOnly = true)
    @Override
    public Page<TransactionDTO> getAccountTransactions(Long accountId, Pageable pageable) {

        Page<Transaction> transactions = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId, pageable);
        Page<TransactionDTO> transactionDTOPage = transactions.map(this::convertEntityToDTO);
//        List<TransactionDTO> transactionDTOList = new ArrayList<>();
//
//        for (Transaction transaction : transactionList) {
//            TransactionDTO transactionDTO = convertEntityToDTO(transaction);
//            transactionDTOList.add(transactionDTO);
//        }

//        List<TransactionDTO> collect =
//                transactions.stream().
//                map(transaction -> convertEntityToDTO(transaction)).
//                collect(Collectors.toList());


        return transactionDTOPage;
    }


    private TransactionDTO convertEntityToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getTimestamp());
    }


}
