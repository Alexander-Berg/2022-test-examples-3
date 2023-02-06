--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-479_domain_admin_rights_test endDelimiter:///
begin
  java_sec.kampfer. add_static_auth_user('YA_STAFF', 436365330);
  java_sec.kampfer. add_static_auth_user('SECURITY_MANAGER', 436365330);
  java_sec.kampfer. add_static_auth_user('YA_STAFF', 436433766);
  java_sec.kampfer. add_static_auth_user('SECURITY_MANAGER', 436433766);
  java_sec.kampfer. add_static_auth_user('YA_STAFF', 436365328);
  java_sec.kampfer. add_static_auth_user('SECURITY_MANAGER', 436365328);
  java_sec.kampfer. add_static_auth_user('YA_STAFF', 436365329);
  java_sec.kampfer. add_static_auth_user('SECURITY_MANAGER', 436365329);

  --yndx-vsubhuman-partner-reader
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (17148402, 436365330);

  --yndx-vsubhuman-mbi-developer
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (17148402, 436433766);

  --yndx-vsubhuman-admin-reports
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (17148402, 436365328);

  --yndx-vsubhuman-admin-reader
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (17148402, 436365329);
end;
///
