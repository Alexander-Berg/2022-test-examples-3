ALTER TABLE SHOPS_WEB.DATASOURCES_STOP_CHECK
 ADD (
  MESSAGE_TID NUMBER,
  MESSAGE_SUBJECT VARCHAR2 (300),
  MESSAGE_BODY VARCHAR2(4000)
 )
/