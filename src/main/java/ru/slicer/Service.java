package ru.slicer;

import ru.slicer.model.Token;
import ru.slicer.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class Service {
    @PersistenceContext(unitName="my-pu")
    protected EntityManager entityManager;

    @Transactional
    public Token login(final String email, final String password) throws ExpectedException {
        final User user;
        try {
            //Пока так, тупо без учета конкуренции, для проверки
            user = (User) entityManager.createQuery("select user from User user where user.email = :email")
                    .setParameter("email", email).getSingleResult();//и без DAO... usersDao.findUserByEmail(email)
        } catch (NoResultException e) {
            throw new ExpectedException("Customer not found", "customer.notFound");
        }
        if ((password == null && user.getPassword() != null) || !user.getPassword().equals(password))
            throw new ExpectedException("Invalid customer password", "customer.invalidPassword");

        final Token token = Token.createNewFor(user);
        entityManager.persist(token);
        return token;
    }
}
