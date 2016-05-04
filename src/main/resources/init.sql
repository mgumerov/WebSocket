-- Основная проблема в том, что повторный деплой приложения без перезапуска сервера вполне
-- может и не перезапускать JVM, а режим "потеря данных при потере последнего подключения"
-- нам использовать неудобно, раз у нас DriverManaged datasource. Поэтому на случай, если
-- на момент запуска приложения БД уже существует in-memory, сбросим ее.
DROP ALL OBJECTS;

CREATE TABLE USERS (
USERNAME VARCHAR2,
PASSWORD VARCHAR2,
ISADMIN NUMBER,
CONSTRAINT pk_users                 PRIMARY KEY (USERNAME)
);

INSERT INTO USERS(USERNAME, PASSWORD, ISADMIN) VALUES ('Admin', 'admin', 1);
INSERT INTO USERS(USERNAME, PASSWORD, ISADMIN) VALUES ('User', 'user', 0);

CREATE TABLE MESSAGES (
SENDER VARCHAR2,
RECIPIENT VARCHAR2,
SENT TIMESTAMP,
SUBJECT VARCHAR2,
BODY VARCHAR2,
CONSTRAINT fk_messages_sender_users FOREIGN KEY (SENDER) REFERENCES USERS(USERNAME),
CONSTRAINT fk_messages_recipient_users FOREIGN KEY (RECIPIENT) REFERENCES USERS(USERNAME)
);

INSERT INTO MESSAGES(SENDER, RECIPIENT, SENT, SUBJECT, BODY) VALUES
  ('Admin', 'User', CURRENT_TIMESTAMP-1, 'Welcome', 'Welcome to our message board'),
  ('Admin', 'User', CURRENT_TIMESTAMP, 'Z is the last one', 'Some random message'),
  ('User', 'Admin', CURRENT_TIMESTAMP, 'Request', 'Another random message');
