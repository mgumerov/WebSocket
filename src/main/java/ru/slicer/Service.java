package ru.slicer;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class Service {
    @Transactional
    public void test() {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        final EntityManagerFactory emf = Persistence.createEntityManagerFactory("my-pu");
        final EntityManager entityManager = emf.createEntityManager();
        final List users = entityManager.createQuery("select user from User").getResultList();
        System.out.println(users.size());
    }
}
