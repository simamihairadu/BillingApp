package net.gsdgroup.billing.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

/**
 * Base repository for all concrete repositories.
 * @param <TEntity>
 */
public abstract class AbstractCommonRepository<TEntity> {

    @Autowired
    protected SessionFactory factory;

    public int add(TEntity entity) {

        return (int) factory.getCurrentSession().save(entity);
    }

    public void delete(TEntity entity) {

        factory.getCurrentSession().delete(entity);
    }

    public void update(TEntity entity) {

        factory.getCurrentSession().update(entity);
    }

    public TEntity getById(Class<TEntity> type,int id) {

        return factory.getCurrentSession().get(type,id);
    }

    public List<TEntity> getAll(Class<TEntity> type) {

        CriteriaBuilder builder = factory.getCurrentSession().getCriteriaBuilder();
        CriteriaQuery<TEntity> criteria = builder.createQuery(type);
        criteria.from(type);
        List<TEntity> list = factory.getCurrentSession().createQuery(criteria).getResultList();
        return list;
    }
}
