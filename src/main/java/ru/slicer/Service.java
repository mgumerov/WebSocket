package ru.slicer;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class Service {
    @PersistenceContext(unitName="my-pu")
    protected EntityManager entityManager;

    @Transactional
    public void test() {
        final List users = entityManager.createQuery("select user from User user").getResultList();
        System.out.println(users.size());
    }
}
