--liquibase formatted sql

--changeset a-danilov:Create-ADM_CLEAR_QUEUE endDelimiter://

--
-- !!! Процедура предназначена только для тестинга
--
CREATE OR REPLACE PROCEDURE wuser.adm_clear_queue(p_table_name varchar2, p_method varchar2)
  /*
  Очищает очередь в тестинге
  = Параметры
   * p_table_name varchar2
   Возможные значения
     * plog_click - очищается очередь кликов
     * click_rollback - очищается очередь откатов
   * p_method varchar2
   Возможные значения
     * all - очищается вся очередь
     * existing - очищается до транзакции, которые успели осесть в сырых кликах
  */
AS
  l_max_tran_id int;
  BEGIN


    IF p_method NOT IN ('all', 'existing') THEN
      raise_application_error(-20001, 'Unknown p_method: ' || p_method
          || ' | Allowed values: all|existing');
    END IF;

    IF p_method = 'all' THEN
      -- грубо скручиваем ее до конца
      SELECT max(to_trans_id)
      INTO l_max_tran_id
      FROM wuser.mst_rcv_queue
      WHERE table_name = p_table_name;
    ELSE
      --   Пытаемся скрутить ее только до транзакций, которые успели осесть в сырых кликах
      EXECUTE IMMEDIATE 'SELECT max(trans_id) FROM wuser.' || p_table_name
      INTO l_max_tran_id;
    END IF;

    -- Делаем вид Что смогли забрать клики и подготовить их к обилливанию ("зароллапить джобы" - и в том числе проставить кликам trantime)
    UPDATE wuser.mst_rcv_queue
    SET to_trans_id = l_max_tran_id
    WHERE table_name = p_table_name AND status = 3;

    -- Если имеем дело с сырыми кликами для обиливания - то надо или почистить невалидно навставленные клики или откаты (очень долго),
    -- или просто скрутить окно обилливания (TRAN_LIMITS), сделав вид, что клики обработали
    UPDATE wuser.tran_limits
    SET
      first_trans_id = l_max_tran_id,
      last_trans_id = l_max_tran_id
    WHERE tablename = upper(p_table_name);

    -- Удаляем из очереди джобы которые надо проигнорировать
    DELETE FROM wuser.mst_rcv_queue
    WHERE table_name = p_table_name AND to_trans_id <= l_max_tran_id AND status <> 3;
  END;