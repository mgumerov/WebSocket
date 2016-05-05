-- Основная проблема в том, что повторный деплой приложения без перезапуска сервера вполне
-- может и не перезапускать JVM, а режим "потеря данных при потере последнего подключения"
-- нам использовать неудобно, раз у нас DriverManaged datasource. Поэтому на случай, если
-- на момент запуска приложения БД уже существует in-memory, сбросим ее.
DROP ALL OBJECTS;

CREATE TABLE USERS (
ID BIGINT auto_increment,
EMAIL VARCHAR2,
PASSWORD VARCHAR2,
CONSTRAINT pk_users                 PRIMARY KEY (ID)
);

INSERT INTO USERS(EMAIL, PASSWORD) VALUES ('admin@foobar.org', 'admin');
INSERT INTO USERS(EMAIL, PASSWORD) VALUES ('user@foobar.org', 'user');

CREATE TABLE TOKENS (
ID BIGINT auto_increment,
USER_ID BIGINT,
TOKEN VARCHAR2,
CONSTRAINT fk_tokens_users FOREIGN KEY (USER_ID) REFERENCES USERS(ID),
CONSTRAINT pk_tokens       PRIMARY KEY (ID)
);
