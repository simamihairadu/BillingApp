package net.gsdgroup.billing.business;

import net.gsdgroup.billing.dao.AccountRepository;
import net.gsdgroup.billing.exceptions.ServiceException;
import net.gsdgroup.billing.webservice.accountDTO.AccountDTO;
import net.gsdgroup.billing.webservice.accountDTO.SimpleAccountDTO;
import net.gsdgroup.billing.webservice.billDTO.BillChargeDTO;
import net.gsdgroup.billing.webservice.billDTO.BillDTO;
import net.gsdgroup.billing.entity.Account;
import net.gsdgroup.billing.entity.Bill;
import net.gsdgroup.billing.entity.BillCharge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.NoResultException;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class that handles basic Account functionality.
 */
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DataValidator dataValidator;

    @Transactional
    public int addAccount(Account account) {

        int accountId = 0;

        if(account == null){
            throw new ServiceException("Error processing request. The account entity cannot be null.");
        }

        if(dataValidator.validateAccountData(account)) {
            accountId = accountRepository.add(account);

        } else {
            throw new ServiceException("Account entity validation error.");
        }

        return accountId;
    }

    @Transactional
    public void deleteAccount(int accountId) {

        Account account = accountRepository.getById(Account.class,accountId);

        if(account == null){
            throw new ServiceException("Account not found.");

        } else {
            accountRepository.delete(account);
        }
    }

    @Transactional
    public void updateAccount(Account account) {

        if(account == null){
            throw new ServiceException("Error processing request. The account entity cannot be null.");
        }

        if(dataValidator.validateAccountData(account)) {
            accountRepository.update(account);

        } else {
            throw new ServiceException("Account entity validation error.");
        }
    }

    @Transactional
    public List<AccountDTO> getAllAccounts(){

        List<AccountDTO> accountDtoList = new ArrayList<>();
        //TODO extract into variable getAll
        for (Account account : accountRepository.getAll(Account.class)) {
            AccountDTO accountDto = buildAccountDto(account);
            accountDtoList.add(accountDto);
        }

        return accountDtoList;
    }

    @Transactional
    public AccountDTO getAccountById(int id) {

        try {
            AccountDTO accountDto = buildAccountDto(accountRepository.getById(id));
            return accountDto;

        } catch (NoResultException e){
            throw new ServiceException("No result.");
        }
    }

    @Transactional
    public List<AccountDTO> getAccountsWithOverdueBills(){

        List<AccountDTO> accountDtoList = new ArrayList<>();
        //TODO extract into variable getAll
        for (Account account: accountRepository.getAccountsWithOverdueBills()) {
            AccountDTO accountDto = buildAccountDto(account);

            accountDtoList.add(accountDto);
        }
        return accountDtoList;
    }

    public AccountDTO buildAccountDto(Account accountEntity){

        AccountDTO accountDto = new AccountDTO();
        accountDto.setId(accountEntity.getId());
        accountDto.setFirstName(accountEntity.getFirstName());
        accountDto.setLastName(accountEntity.getLastName());

        for (Bill bill : accountEntity.getBills()){

            BillDTO billDTO = new BillDTO();
            billDTO.setId(bill.getId());
            billDTO.setIssueDate(bill.getIssueDate());
            billDTO.setDueDate(bill.getDueDate());
            billDTO.setAccount(accountDto);

            for (BillCharge billCharge : bill.getBillCharges()){

                BillChargeDTO billChargeDto = new BillChargeDTO();
                billChargeDto.setId(billCharge.getId());
                billChargeDto.setChargeType(billCharge.getChargeType());
                billChargeDto.setAmount(billCharge.getAmount());
                billChargeDto.setTax(billCharge.getTax());
                billChargeDto.setBill(billDTO);

                billDTO.getBillCharges().add(billChargeDto);
            }

            accountDto.getBills().add(billDTO);
        }

        return accountDto;
    }

    public Account buildAccountEntity(SimpleAccountDTO simpleAccountDTO){

        Account accountEntity = new Account();

        try{
            if(simpleAccountDTO.getId() != 0) {
                accountEntity.setId(simpleAccountDTO.getId());
            }

            accountEntity.setFirstName(simpleAccountDTO.getFirstName());
            accountEntity.setLastName(simpleAccountDTO.getLastName());

        } catch (NullPointerException e){

            throw new ServiceException("Error processing request. The account entity cannot be null.");
        }

        return accountEntity;
    }

    public Account buildAccountEntity(AccountDTO accountDTO){

        if(accountDTO == null){

            throw new ServiceException("Error processing request. The account entity cannot be null.");
        }

        Account accountEntity = new Account();
        accountEntity.setId(accountDTO.getId());
        accountEntity.setFirstName(accountDTO.getFirstName());
        accountEntity.setLastName(accountDTO.getLastName());

        for (BillDTO billDTO : accountDTO.getBills()){

            Bill bill = new Bill();
            bill.setId(billDTO.getId());
            bill.setIssueDate(billDTO.getIssueDate());
            bill.setDueDate(billDTO.getDueDate());
            bill.setAccount(accountEntity);

            for (BillChargeDTO billChargeDTO : billDTO.getBillCharges()){

                BillCharge billCharge = new BillCharge();
                billCharge.setId(billChargeDTO.getId());
                billCharge.setChargeType(billChargeDTO.getChargeType());
                billCharge.setAmount(billChargeDTO.getAmount());
                billCharge.setTax(billChargeDTO.getTax());
                billCharge.setBill(bill);

                bill.getBillCharges().add(billCharge);
            }

            accountEntity.getBills().add(bill);
        }
        return accountEntity;
    }
}
