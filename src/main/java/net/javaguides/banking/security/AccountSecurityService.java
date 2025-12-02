//package net.javaguides.banking.security;
//
//
//import net.javaguides.banking.entity.Account;
//import net.javaguides.banking.repository.AccountRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Service;
//
//@Service("accountSecurityService")
//public class AccountSecurityService {
//
//    @Autowired
//    private AccountRepository accountRepository;
//
//
//    public boolean isOwner(Authentication authentication, Long accountId) {
//
//        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("accountId nit found"));
//
//        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
//
//        if (account.getUser() == null) {
//            return false;
//        }
//
//        return userDetails.getId().equals(account.getUser().getUserId());
//    }
//
//    public boolean canCreateAccountFor(Authentication authentication, String username) {
//        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
//        return userDetails.getUsername().equals(username);
//    }
//
//}
