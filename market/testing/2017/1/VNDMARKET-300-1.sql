--liquibase formatted sql
--Добавление нового продукта в вендоров и балансового айдишника в коробку IN TESTING
--changeset vladimir-k:VNDMARKET-300-product-testing endDelimiter:///
BEGIN

  INSERT INTO VENDORS.PRODUCT (ID, NAME, BALANCE_ID)
  VALUES (2, 'Ставки на модели', 507934);

  INSERT INTO CS_BILLING.PARAM_VALUE (CS_ID, PARAM_TYPE_ID, NUM_VALUE)
  VALUES (132, 5, 507934);

END;
///