--liquibase formatted sql

--changeset vekshin-vlad:VNDMARKET-7571-add-new-product endDelimiter:///
BEGIN
    EXECUTE IMMEDIATE 'INSERT INTO VENDORS.PRODUCT (ID, NAME, BALANCE_ID)
                       VALUES (16, ''Медийные услуги (Врезки)'', 508570)';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
///
