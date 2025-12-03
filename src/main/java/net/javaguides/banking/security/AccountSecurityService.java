package net.javaguides.banking.security;


import net.javaguides.banking.entity.Account;
import net.javaguides.banking.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("accountSecurityService")
public class AccountSecurityService {

    @Autowired
    private AccountRepository accountRepository;


    public boolean isOwner(Authentication authentication, Long accountId) {

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("accountId not found"));

       Jwt jwt=(Jwt) authentication.getPrincipal();

        String uuid = jwt.getClaimAsString("sub");


        if (account.getUser() == null) {
            return false;
        }

        return uuid.equals(account.getUser().getUserId());
    }



}
