package ru.slicer;

import ru.slicer.model.Token;
import ru.slicer.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class Service {
    @PersistenceContext(unitName="my-pu")
    protected EntityManager entityManager;

    @Transactional
    public List<Token> dump(final String email) {
        return entityManager.createQuery("select token from Token token where token.user.email = :email")
                .setParameter("email", email).getResultList();
    }

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

        //Тут есть важный нюанс - пока я не вводил на уровне БД ограничение уникальности активного токена для
        //фиксированного юзера. В таком виде, как сейчас - когда нет в TOKENS поля disabled - можно считать, что
        //действующими являются те токены, у которых время конца действия меньше текущей; а чтобы сбросить токен,
        //можно срезать ему дату-время окончания действия до текущей (или например на 01.01.2000). Но тогда на уровне
        //БД нет защиты от ситуации, когда у двух токенов дата окончания не сброшена. Ну или даже если СУБД будет
        //такой констрейнт поддерживать, проверка сравнением с текущей датой - не очень красиво, ведь она меняется.
        //
        //Как вариант, сброс можно было бы делать флагом disabled, и ввести ограничение, по которому для юзера лишь
        //одна запись может иметь disabled = false (ну или not null например). Как минимум, такое можно проделать в
        //оракле, и вроде в постгре примерно так же. Но тогда возникает интересная проблема: если две транзакции
        //почти одновременно запросили аутентификацию одного и того же юзера, то условно "первая" закоммитит результат,
        //а вторая упадет с ошибкой нарушения уникальности, а ведь обе вполне могли бы выполниться успешно, просто
        //"вторая" бы отменила действие "первой". Тут нужно будет придумывать какое-то решение, чтобы этого избежать.
        //Ну хотя бы просто повторять заново неудавшуюся транзакцию.
        //
        //Пока для экономии времени я не пойду этим путем (disabled) и решу, что защиту со стороны БД выставить нельзя.
        //Тогда ограничимся защитой со стороны Java. Можно было бы сделать наивно: сначала добавить новый активный
        //токен; затем пометить как неактивные все другие токены данного юзера. В условиях гонки между добавлением
        //токена A и Б операция "сброса" всех кроме А окажется несовместима с операцией добавления Б, и возможен даже
        //дедлок. Можно было бы сбрасывать все "другие" записи, которые в данный момент активны, чтобы избежать дедлока;
        //но тогда транзакции вообще друг другу не будут мешать, ведь в худшем случае они вообще не увидят добавленных
        //друг другом записей, если закоммитятся одновременно - и тогда не будут ничего сбрасывать.

        //Другим лечением классического дедлока может быть сначала "сброс" сразу всех записей по данному юзеру - при
        //условии, что в таблице токенов имеется
        //индекс по юзеру и БД достаточно умная, чтобы уметь range lock (все нормальные БД вроде умеют), это приведет
        //к тому, что другие такие транзакции по тому же юзеру будут ждать окончания нашей и не смогут тоже проделать
        //сброс. А уже после этого мы спокойно добавим новый токен по тому же юзеру. Но если мы начали с того, что
        //реально провели сброс даты окончания в каких-то существующих записях в одной транзакции, другая в итоге не
        //сможет закоммититься, так что это не решение. В SQL решением было бы не изменять их сразу, а сделать
        //select for update, а уже потом (пока конкурентные транзакции ждут нас) сбросить все токены по юзеру,
        //добавить новый, закоммититься. Но в чистом JPA так можно лочить - не изменяя - только ОДНУ запись!
        //А я хочу остаться в рамках JPA.
        //Я думаю, все равно пойду этим путем. Залочу User (да, это плохая идея в реальном приложении, менять User может
        //быть нужно и тем, кто реально не конкурирует с нами, но в реальном приложении я бы не поленился и завел
        //отдельную табличку именно для блокировок tokens по user, где бы выписывал user-ов, которым хоть один token
        //был назначен, и в ней бы блокировал).
        entityManager.find(User.class, user.getId(), LockModeType.PESSIMISTIC_READ);
        //теперь никто не мешает нам. Можем поменять нужное.
        entityManager.persist(token);
        entityManager
                .createQuery("update Token token set token.expires=CURRENT_TIMESTAMP " +
                        "where (token.id <> :tokenid) and (token.user.id = :userid)")
                .setParameter("tokenid", token.getId())
                .setParameter("userid", user.getId())
                .executeUpdate();

        return token;
    }
}
