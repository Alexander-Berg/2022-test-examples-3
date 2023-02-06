--liquibase formatted sql

--changeset rpanasenkov:VNDMARKET-2742_add_admin_rights_to_rpanasenkov
DECLARE
  uid_yndx_rpanasenkov CONSTANT NUMBER := 849589468; --yndx-rpanasenkov
BEGIN
  INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (17148402, uid_yndx_rpanasenkov);
END;
/
