--liquibase formatted sql

--changeset ogonek:VNDMARKET-2975_add_admin_rights_to_analback.sql
DECLARE
  uid_yndx_ogonek CONSTANT NUMBER := 672777019; --yndx-ogonek
  uid_yndx_fbokovikov CONSTANT NUMBER := 411814423; --yndx-fbokovikov
  uid_yndx_antipov93 CONSTANT NUMBER := 421758995; --yndx-antipov93
  cs_partner_206_testing CONSTANT NUMBER := 18272787;
BEGIN
  java_sec.kampfer.add_static_auth_user('YA_STAFF', uid_yndx_ogonek);
  java_sec.kampfer.add_static_auth_user('SECURITY_MANAGER', uid_yndx_ogonek);
  java_sec.kampfer.add_static_auth_user('YA_STAFF', uid_yndx_fbokovikov);
  java_sec.kampfer.add_static_auth_user('SECURITY_MANAGER', uid_yndx_fbokovikov);
  java_sec.kampfer.add_static_auth_user('YA_STAFF', uid_yndx_antipov93);
  java_sec.kampfer.add_static_auth_user('SECURITY_MANAGER', uid_yndx_antipov93);

  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (cs_partner_206_testing, uid_yndx_ogonek);
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (cs_partner_206_testing, uid_yndx_fbokovikov);
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (cs_partner_206_testing, uid_yndx_antipov93);

END;
/
