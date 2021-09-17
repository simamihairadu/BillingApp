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

        StringBuilder query = new StringBuilder("SELECT DISTINCT ac " +
                "FROM Account ac " +
                "LEFT JOIN FETCH ac.bills b " +
                "WHERE b.dueDate < CURRENT_DATE");

        return factory.createEntityManager()
                .createQuery(query.toString(),Account.class)
                .getResultList();
    }

    public Account getById(int id){

        StringBuilder query = new StringBuilder("SELECT ac " +
                "FROM Account ac " +
                "LEFT JOIN FETCH ac.bills " +
                "WHERE ac.id = :id");

        return factory.createEntityManager().createQuery(query.toString(),Account.class)
                    .setParameter("id", id).getSingleResult();
    }
}
