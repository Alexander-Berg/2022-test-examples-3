CREATE TABLE JAVA_SEC.AUTH_LINK_TEMP_AUDIT (
  AUDIT_RECORD_ID NUMBER NOT NULL,
  AUDIT_RECORD_TYPE VARCHAR(32),
  CHANGE_TIME TIMESTAMP,
  CHANGE_USER_ID NUMBER,
  IP VARCHAR(160),
  ID NUMBER NOT NULL,
  MAIN_AUTH VARCHAR(320) NOT NULL,
  LINKED_AUTH VARCHAR(320) NOT NULL,
  REL VARCHAR(12) NOT NULL,
  LINKED_AUTH_PARAM VARCHAR(4000),
  MAIN_AUTH_ID NUMBER,
  LINKED_AUTH_ID NUMBER
);


