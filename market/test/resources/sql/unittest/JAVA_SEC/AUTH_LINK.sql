CREATE TABLE JAVA_SEC.AUTH_LINK (
  ID NUMBER NOT NULL,
  MAIN_AUTH VARCHAR(320),
  LINKED_AUTH VARCHAR(320),
  REL VARCHAR(12) NOT NULL,
  LINKED_AUTH_PARAM VARCHAR(4000),
  MAIN_AUTH_ID NUMBER NOT NULL,
  LINKED_AUTH_ID NUMBER NOT NULL
);

CREATE UNIQUE INDEX JAVA_SEC.SYS_C0026342 ON JAVA_SEC.AUTH_LINK (ID);

ALTER TABLE JAVA_SEC.AUTH_LINK ADD CONSTRAINT SYS_C0026342 PRIMARY KEY (ID);
