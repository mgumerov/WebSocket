package ru.slicer.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;

@Entity
@Table(name="TOKENS")
//Терминологию примем такую. Токен (как монетка или билет) содержит номер, срок действия, "кому выдан" и т.п.
//Тогда просто номер - это token id, а токен целиком включает token id и прочее.
public class Token {
    @Id
    private String id;

    //С момента expires токен уже считается недействительным
    private Timestamp expires;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Timestamp getExpirationDate() {
        return new Timestamp(expires.getTime());
    }

    public String getId() {
        return id;
    }

    public static Token createNewFor(User user) {
        final Token token = new Token();
        token.user = user;
        token.id = UUID.randomUUID().toString();
        final Calendar cal = Calendar.getInstance(); //heavy but precise
        cal.add(Calendar.DAY_OF_MONTH, 1);
        token.expires = new Timestamp(cal.getTime().getTime());
        return token;
    }
}
