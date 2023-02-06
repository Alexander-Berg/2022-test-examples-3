--liquibase formatted sql

--changeset rpanasenkov:VNDMARKET-2742_add_admin_rights_dolgova
DECLARE
    uid_yndx_dolgova CONSTANT NUMBER := 404486454; --yndx-dolgova
BEGIN
    INSERT INTO java_sec.domain_admins (domain_id, user_id) VALUES (17148402, uid_yndx_dolgova);
END;
/
