package ru.slicer.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="USERS")
public class User {
    @Id
    private long id;

    private String email;
    private String password;

//            ID BIGINT auto_increment,
  //          EMAIL VARCHAR2,
    //        PASSWORD VARCHAR2,

}
