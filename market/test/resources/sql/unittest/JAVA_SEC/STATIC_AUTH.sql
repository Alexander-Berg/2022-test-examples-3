CREATE TABLE JAVA_SEC.STATIC_AUTH (
  ID NUMBER NOT NULL,
  USER_ID NUMBER NOT NULL,
  AUTH_NAME VARCHAR(320) NOT NULL
);

CREATE UNIQUE INDEX JAVA_SEC.SYS_C0026343 ON JAVA_SEC.STATIC_AUTH (ID);

ALTER TABLE JAVA_SEC.STATIC_AUTH ADD CONSTRAINT SYS_C0026343 PRIMARY KEY (ID);
