-- �������� �������� � ���, ��� ��������� ������ ���������� ��� ����������� ������� ������
-- ����� � �� ������������� JVM, � ����� "������ ������ ��� ������ ���������� �����������"
-- ��� ������������ ��������, ��� � ��� DriverManaged datasource. ������� �� ������, ����
-- �� ������ ������� ���������� �� ��� ���������� in-memory, ������� ��.
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
