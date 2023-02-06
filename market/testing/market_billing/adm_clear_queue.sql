--liquibase formatted sql

--changeset kudrale:MBI-15348 endDelimiter://
--
-- !!! Процедура предназначена только для тестинга
--
CREATE OR REPLACE PROCEDURE market_billing.adm_clear_queue(p_table_name varchar2)
  /*
  Очищает очередь в тестинге
  = Параметры
   * p_table_name varchar2
  */
AS
  l_max_tran_id int;
  BEGIN

      SELECT max(to_trans_id)
      INTO l_max_tran_id
      FROM market_billing.mst_rcv_queue
      WHERE table_name = p_table_name;

    -- Делаем вид Что смогли забрать клики и подготовить их к обилливанию ("зароллапить джобы" - и в том числе проставить кликам trantime)
    UPDATE market_billing.mst_rcv_queue
    SET to_trans_id = l_max_tran_id
    WHERE table_name = p_table_name AND status = 3;

    -- Удаляем из очереди джобы которые надо проигнорировать
    DELETE FROM market_billing.mst_rcv_queue
    WHERE table_name = p_table_name AND to_trans_id <= l_max_tran_id AND status <> 3;
  END;

//
