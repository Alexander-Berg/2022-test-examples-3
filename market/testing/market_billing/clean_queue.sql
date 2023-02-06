--liquibase formatted sql

--changeset kudrale:MBI-15348 endDelimiter://
CREATE OR REPLACE PROCEDURE market_billing.clean_queue(i_table_name IN VARCHAR2)
IS
  min_broken_id        NUMBER;
  broken_to_trans_id   NUMBER;
  broken_from_trans_id NUMBER;
  prev_ok_id           NUMBER;
  prev_ok_status       NUMBER;
  BEGIN
    SELECT id, from_trans_id, to_trans_id
    INTO min_broken_id, broken_from_trans_id, broken_to_trans_id
    FROM MST_RCV_QUEUE WHERE FROM_TRANS_ID = (SELECT min(FROM_TRANS_ID) FROM MST_RCV_QUEUE WHERE table_name = LOWER(i_table_name) AND status = 2);

    DELETE FROM MST_RCV_QUEUE WHERE id = min_broken_id;

    SELECT id, status
    INTO prev_ok_id, prev_ok_status
    FROM MST_RCV_QUEUE WHERE TO_TRANS_ID = (SELECT max(TO_TRANS_ID) FROM MST_RCV_QUEUE WHERE table_name = LOWER(i_table_name) AND TO_TRANS_ID < broken_from_trans_id);

    UPDATE mst_rcv_queue SET TO_TRANS_ID = broken_to_trans_id WHERE id = prev_ok_id;

    dbms_output.put_line('prev_ok_id = ' || prev_ok_id || ', min_broken_id = '|| min_broken_id);
  END;
//
