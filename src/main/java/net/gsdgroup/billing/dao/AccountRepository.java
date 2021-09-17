package net.gsdgroup.billing.dao;

import net.gsdgroup.billing.entity.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public class AccountRepository extends AbstractCommonRepository<Account> {

    public List<Account> getAccountsWithOverdueBills(){
        //TODO separate string query
        return factory.createEntityManager().createQuery("SELECT DISTINCT ac " +
                "FROM Account ac " +
                "LEFT JOIN FETCH ac.bills b " +
                "WHERE b.dueDate < CURRENT_DATE",Account.class)
                .getResultList();
    }

    public Account getById(int id){

        return factory.createEntityManager().createQuery("SELECT ac FROM Account ac LEFT JOIN FETCH ac.bills WHERE ac.id = :id",Account.class)
                    .setParameter("id", id).getSingleResult();
    }
}
