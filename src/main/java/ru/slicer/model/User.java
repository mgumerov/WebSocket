package ru.slicer.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="USERS")
public class User {
    @Id //TODO продекларировать автогенерацию
    private long id;

    private String email;
    private String password;

    public String getPassword() {
        return password;
    }
}
