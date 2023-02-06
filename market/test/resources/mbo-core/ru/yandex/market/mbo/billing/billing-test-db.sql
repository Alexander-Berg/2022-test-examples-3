CREATE TABLE ng_paid_operation (
     id INT,
     descr VARCHAR(128) NOT NULL,

     CONSTRAINT ng_paid_operation PRIMARY KEY (id)
);
INSERT INTO ng_paid_operation (id, descr) VALUES (0, 'Test descr');
INSERT INTO ng_paid_operation (id, descr) VALUES (1, 'Test descr 2');

CREATE SEQUENCE ng_paid_operation_id_seq
     START WITH 0
     INCREMENT BY 1
     MINVALUE 0
     NOMAXVALUE
     NOCACHE;


-------------------------------------------------------------------------------
CREATE TABLE ng_operation_tarif (
     operation_id INT,
     price NUMBER(16, 8) NOT NULL,
     since DATE NOT NULL,

     CONSTRAINT ng_ot_operation_id_fk FOREIGN KEY (operation_id) REFERENCES ng_paid_operation(id)
);

-------------------------------------------------------------------------------
CREATE TABLE ng_category_operation_tarif (
     operation_id INT,
     category_id INT NOT NULL,
     price NUMBER(16, 8) NOT NULL,
     since DATE NOT NULL,

     CONSTRAINT ng_cot_operation_id_fk FOREIGN KEY (operation_id) REFERENCES ng_paid_operation(id)
);

-------------------------------------------------------------------------------
CREATE TABLE ng_paid_operation_log (
     id INT,
     user_id INT NOT NULL,
     category_id INT,
     time DATE NOT NULL,
     operation_id INT,
     price NUMBER(16, 8) NOT NULL,
     source_id INT NOT NULL,
     count NUMBER DEFAULT 1 NOT NULL,
     audit_action_id NUMBER,
     external_source VARCHAR2(32),
     external_source_id VARCHAR2(128),
     parameter_name VARCHAR2(4000),

       CONSTRAINT ng_paid_operation_log PRIMARY KEY (id),
     CONSTRAINT ng_pol_operation_id_fk FOREIGN KEY (operation_id) REFERENCES ng_paid_operation(id)
);

CREATE SEQUENCE ng_paid_operation_log_id_seq
     START WITH 0
     INCREMENT BY 1
     MINVALUE 0
     NOMAXVALUE
     NOCACHE;

-------------------------------------------------------------------------------
CREATE TABLE ng_paid_operation_log_archive (
     id INT,
     user_id INT NOT NULL,
     category_id INT,
     time DATE NOT NULL,
     operation_id INT,
     price NUMBER(16, 8) NOT NULL,
     source_id INT NOT NULL,
     count NUMBER DEFAULT 1 NOT NULL,
     audit_action_id NUMBER,
     external_source VARCHAR2(32),
     external_source_id VARCHAR2(128),
     parameter_name VARCHAR2(4000),

       CONSTRAINT ng_paid_operation_log_archive PRIMARY KEY (id)
);

-------------------------------------------------------------------------------
CREATE TABLE ng_suspended_operation_log (
     id INT,
     user_id INT NOT NULL,
     category_id INT,
     time DATE NOT NULL,
     operation_id INT,
     source_id INT NOT NULL,
     count NUMBER DEFAULT 1 NOT NULL,
     external_source VARCHAR2(32),
     external_source_id VARCHAR2(128),
     parameter_name VARCHAR2(4000),
     price_multiplicator NUMBER(16, 8),

       CONSTRAINT ng_suspended_operation_log PRIMARY KEY (id),
     CONSTRAINT ng_sol_operation_id_fk FOREIGN KEY (operation_id) REFERENCES ng_paid_operation(id)
);

CREATE SEQUENCE ng_suspended_operation_id_seq
     START WITH 0
     INCREMENT BY 1
     MINVALUE 0
     NOMAXVALUE
     NOCACHE
;

CREATE TABLE ng_daily_balance (
     id INT,
     day DATE NOT NULL,
     balance NUMBER(16, 8) NOT NULL,

       CONSTRAINT ng_daily_balance PRIMARY KEY (id)
);

CREATE SEQUENCE ng_daily_balance_id_seq
     START WITH 0
     INCREMENT BY 1
     MINVALUE 0
     NOMAXVALUE
     NOCACHE;

INSERT INTO ng_daily_balance (id, day, balance) VALUES (ng_daily_balance_id_seq.NEXTVAL, '2007-06-01', 0);

--------------------------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ng_billing_session (
     begin,
     end
)
AS
SELECT
     MAX(day) AS begin,
     (MAX(day) + 1) AS end
FROM ng_daily_balance;

--------------------------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ng_billing_balance (
     balance
)
AS
SELECT
    ng_daily_balance.balance
FROM
    ng_daily_balance
WHERE
    day = (SELECT MAX(day) FROM ng_daily_balance);

--------------------------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ng_billing_delta (
     delta
)
AS
SELECT
     (SELECT SUM(price * count) FROM ng_paid_operation_log) -
     (SELECT balance FROM v_ng_billing_balance)
FROM DUAL;

--------------------------------------------------------------------------------------------
CREATE TABLE ng_billing_status (
     start_date DATE NOT NULL,
     status VARCHAR2(10),
     updated TIMESTAMP,

     CONSTRAINT ng_billing_status PRIMARY KEY (start_date)
);
