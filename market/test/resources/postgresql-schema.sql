CREATE TABLE IF NOT EXISTS perfomance_history
(
    warehouse      VARCHAR(10) NOT NULL,
    operationGroup VARCHAR(100) NOT NULL,
    operator       VARCHAR(100) NOT NULL,
    company        VARCHAR(20) NOT NULL,
    experience     VARCHAR(50) NOT NULL,
    user_role      VARCHAR(20) NOT NULL,
    qty            BIGINT,
    work_hour      TIMESTAMP NOT NULL,
    supervision    VARCHAR(30) NOT NULL,
    ye             FLOAT,
    ye_per_shift   FLOAT,
    ye_abs         FLOAT,
    operDay        DATE NOT NULL,
    effective      VARCHAR(30),
    result         INTEGER,
    spent_time     INTEGER,
    PRIMARY KEY (operDay, supervision, operationGroup, operator, work_hour)
);

CREATE TABLE IF NOT EXISTS perfomance_dict
(
    ye_per_shift     NUMERIC (13),
    operation_number VARCHAR(3) NOT NULL,
    operation_descr  VARCHAR(1000),
    operation_type   VARCHAR(16),
    count_data_type  VARCHAR(16),
    unit             VARCHAR(4),
    time_unit        INTEGER,
    ye               FLOAT,
    ye_SG            FLOAT,
    ye_box           FLOAT,
    ye_SKU           FLOAT,
    ye_loc           FLOAT,
    price            FLOAT,
    warehouse        VARCHAR(10) NOT NULL,
    PRIMARY KEY (warehouse, operation_number)
);

CREATE TABLE IF NOT EXISTS performance_per_hour
(
    work_hour TIMESTAMP,
    username  VARCHAR(100),
    overall   FLOAT
);

CREATE TABLE IF NOT EXISTS AREA
(
    id   BIGINT         auto_increment not null,
    name VARCHAR(50)    NOT NULL,
    INSIDE              BOOLEAN,
    primary key (id)
);



CREATE TABLE IF NOT EXISTS WAREHOUSES
(
    id                      BIGINT      auto_increment not null,
    name                    VARCHAR(10) NOT NULL,
    TIMEZONE                VARCHAR(50) NOT NULL,
    WMS_WAREHOUSE_CODE      VARCHAR(15),
    code                    BIGINT,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS PUTAWAYZONE
(
    id          BIGINT          auto_increment not null,
    warehouseId BIGINT          NOT NULL references WAREHOUSES (id),
    NAME        VARCHAR(100)    NOT NULL,
    PRIMARY KEY (ID),
    UNIQUE (warehouseId, NAME)
);

CREATE TABLE IF NOT EXISTS EMPLOYEERELOCATION
(
    id          BIGINT       auto_increment not null,
    warehouseId BIGINT       NOT NULL references WAREHOUSES (id),
    username    VARCHAR(100) NOT NULL,
    position    VARCHAR(100) NOT NULL,
    eventtime   datetime2    NOT NULL,
    isentry     BOOLEAN,
    areaId      BIGINT      NOT NULL references AREA (id),
    primary key (id)

);

CREATE UNIQUE INDEX ON EMPLOYEERELOCATION (warehouseId, username, eventtime);

CREATE TABLE IF NOT EXISTS CURRENT_EMPLOYEE_PROCESS_TYPE
(
    id              BIGINT          auto_increment not null,
    warehouseId     BIGINT          NOT NULL references WAREHOUSES (id),
    username        VARCHAR(100)    NOT NULL,
    eventtime       datetime2       NOT NULL,
    PROCESSTYPE     VARCHAR(100)    NOT NULL,
    ASSIGMENTTYPE   VARCHAR(100)    NOT NULL,
    ASSIGNER        VARCHAR(150)   NOT NULL,
    AREA            VARCHAR(100),
    PUTAWAYZONEID   BIGINT          REFERENCES PUTAWAYZONE (id),
    primary key (id)
);

CREATE UNIQUE INDEX ON CURRENT_EMPLOYEE_PROCESS_TYPE (warehouseId, username);

CREATE TABLE IF NOT EXISTS EMPLOYEE_PROCESS_TYPE_HISTORY
(
    id              BIGINT          auto_increment not null,
    warehouseId     BIGINT          NOT NULL REFERENCES WAREHOUSES (id),
    username        VARCHAR(100)    NOT NULL,
    eventtime       datetime2       NOT NULL,
    PROCESSTYPE     VARCHAR(100)    NOT NULL,
    ASSIGMENTTYPE   VARCHAR(100)    NOT NULL,
    ASSIGNER        VARCHAR(150)    NOT NULL,
    AREA            VARCHAR(100),
    PUTAWAYZONEID   BIGINT          REFERENCES PUTAWAYZONE (id),
    primary key (id)
);

CREATE UNIQUE INDEX ON EMPLOYEE_PROCESS_TYPE_HISTORY (warehouseId, username, eventtime);

CREATE TABLE IF NOT EXISTS EMPLOYEE_PROCESS_TYPE_HISTORY
(
    id            BIGINT auto_increment not null,
    warehouseId   BIGINT                NOT NULL references WAREHOUSES (id),
    username      VARCHAR(100)          NOT NULL,
    eventtime     datetime2             NOT NULL,
    PROCESSTYPE   VARCHAR(100)          NOT NULL,
    ASSIGMENTTYPE VARCHAR(100)          NOT NULL,
    ASSIGNER      VARCHAR(150)          NOT NULL,
    AREA          VARCHAR(100),
    primary key (id)
    );

CREATE UNIQUE INDEX ON EMPLOYEE_PROCESS_TYPE_HISTORY (warehouseId, username, eventtime);

CREATE TABLE IF NOT EXISTS EMPLOYEE_STATUS_CURRENT
(
    ID          BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    USERNAME    VARCHAR(100)          NOT NULL,
    EVENTTIME   TIMESTAMP             NOT NULL,
    STATUS      VARCHAR(100)          NOT NULL,
    SUBSTATUS      VARCHAR(100)       NOT NULL DEFAULT '',
    ASSIGNER    VARCHAR(150)          NOT NULL,
    ENDTIME     datetime2,
    AREA        VARCHAR(100)
);

CREATE UNIQUE INDEX ON EMPLOYEE_STATUS_CURRENT (WAREHOUSEID, USERNAME);

CREATE TABLE IF NOT EXISTS EMPLOYEE_STATUS_HISTORY
(
    ID          BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    USERNAME    VARCHAR(100)          NOT NULL,
    EVENTTIME   TIMESTAMP             NOT NULL,
    STATUS      VARCHAR(100)          NOT NULL,
    SUBSTATUS   VARCHAR(100)          NOT NULL DEFAULT '',
    ASSIGNER    VARCHAR(150)          NOT NULL,
    ENDTIME     datetime2,
    AREA        VARCHAR(100)
);

CREATE UNIQUE INDEX ON EMPLOYEE_STATUS_HISTORY (WAREHOUSEID, USERNAME, EVENTTIME);

CREATE TABLE IF NOT EXISTS process
(
    id   INT          PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS process_stats
(
    whs_id        INT,
    process_id    INT REFERENCES process(id),
    num_of_people INT NOT NULL,
    change        INT NOT NULL,
    PRIMARY KEY(whs_id, process_id)
);

CREATE TABLE IF NOT EXISTS whs_mapping
(
    tts_whs_id       INT PRIMARY KEY,
    robokotov_whs_id INT UNIQUE
);

CREATE TABLE IF NOT EXISTS outstaff_company
(
    id     SERIAL      PRIMARY KEY,
    name   VARCHAR(50) NOT NULL,
    prefix VARCHAR(10) NOT NULL,
    whs_id      BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS process_stats_outstaff
(
    id            SERIAL PRIMARY KEY,
    whs_id        INT,
    company_id    INT REFERENCES outstaff_company(id),
    process_id    INT REFERENCES process(id),
    num_of_people INT NOT NULL,
    change        INT NOT NULL,
    UNIQUE (whs_id, company_id, process_id)
);

CREATE TABLE IF NOT EXISTS EMPLOYEE_STATUS_CURRENT
(
    id              BIGINT          auto_increment not null,
    warehouseId     BIGINT          NOT NULL references WAREHOUSES (id),
    username        VARCHAR(100)    NOT NULL,
    eventtime       datetime2       NOT NULL,
    STATUS          VARCHAR(100) NOT NULL,
    ASSIGNER        VARCHAR(150) NOT NULL,
    ENDTIME         datetime2,
    PRIMARY KEY (ID)
    );


--changeset avsimanov:employee-status-current-4
CREATE UNIQUE INDEX ON EMPLOYEE_STATUS_CURRENT (warehouseId, username);

CREATE TABLE IF NOT EXISTS current_shift
(
    username    VARCHAR(100) PRIMARY KEY,
    whs_id      BIGINT       NOT NULL,
    position    VARCHAR(100) NOT NULL,
    shift_start TIMESTAMP    NOT NULL,
    shift_end   TIMESTAMP    NOT NULL,
    shift_name  VARCHAR(100) NOT NULL
    );

CREATE TABLE IF NOT EXISTS EMPLOYEE_STATUS_PLAN
(
    ID          BIGINT AUTO_INCREMENT   NOT NULL PRIMARY KEY,
    WAREHOUSEID BIGINT                  NOT NULL REFERENCES WAREHOUSES (ID),
    STATUS      VARCHAR(100)            NOT NULL,
    COUNT       BIGINT                  NOT NULL,
    LAST_UPDATE datetime2               NOT NULL,
    USERNAME    VARCHAR(100)
);

CREATE UNIQUE INDEX ON EMPLOYEE_STATUS_PLAN (WAREHOUSEID, STATUS);

CREATE TABLE IF NOT EXISTS ASSIGN_TASK
(
    ID                  BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID         BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    USERNAME            VARCHAR(100)          NOT NULL,
    EVENTTIME           datetime2             NOT NULL,
    ASSIGN_STATUS       VARCHAR(100)          NOT NULL,
    ASSIGNER            VARCHAR(150)          NOT NULL,
    DURATION            BIGINT                NOT NULL
    );

CREATE UNIQUE INDEX ON ASSIGN_TASK (WAREHOUSEID, USERNAME, EVENTTIME);

CREATE TABLE IF NOT EXISTS EMPLOYEE_STATUS_PLAN_HISTORY
(
    ID                  BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID         BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    EVENTTIME           datetime2             NOT NULL,
    STATUS              VARCHAR(100)           NOT NULL,
    PLAN                BIGINT                NOT NULL,
    FACT                BIGINT                NOT NULL
);

CREATE UNIQUE INDEX ON EMPLOYEE_STATUS_PLAN_HISTORY (WAREHOUSEID, EVENTTIME, STATUS);

CREATE TABLE IF NOT EXISTS INDIRECT_ACTIVITY_TO_STATUS_MAPPING
(
    ID                  BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID         BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    ACTIVITY_NAME       VARCHAR(150)          NOT NULL,
    STATUS              VARCHAR(100)          NOT NULL
);

CREATE UNIQUE INDEX ON INDIRECT_ACTIVITY_TO_STATUS_MAPPING(WAREHOUSEID, ACTIVITY_NAME, STATUS);

CREATE TABLE IF NOT EXISTS SYSTEM_ACTIVITY_ASSIGNED
(
    ID                  BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID         BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    USERNAME            VARCHAR(150)    NOT NULL,
    PROCESS             VARCHAR(50)     NOT NULL,
    ASSIGNER            VARCHAR(150)    NOT NULL,
    CREATE_TIME         datetime2       NOT NULL,
    EXPECTED_END_TIME   datetime2,
    ZONE                VARCHAR(100)
);

CREATE UNIQUE INDEX ON SYSTEM_ACTIVITY_ASSIGNED (WAREHOUSEID, USERNAME);

CREATE TABLE IF NOT EXISTS BEGINNER_USER
(
    ID              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    WAREHOUSEID     BIGINT                NOT NULL REFERENCES WAREHOUSES (ID),
    USERNAME        VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX ON BEGINNER_USER (WAREHOUSEID, USERNAME);

CREATE TABLE IF NOT EXISTS SHEDLOCK
(
    NAME       VARCHAR(64) PRIMARY KEY,
    LOCK_UNTIL TIMESTAMP,
    LOCKED_AT  TIMESTAMP,
    LOCKED_BY  VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS MANAGER_TO_EMPLOYEE
(
    ID          SERIAL       NOT NULL,
    WAREHOUSE_ID BIGINT       NOT NULL,
    MANAGER     VARCHAR(255) NOT NULL,
    EMPLOYEE    VARCHAR(255) NOT NULL,

    PRIMARY KEY (ID),
    CONSTRAINT FK_BEGINNER_USER_WAREHOUSE_ID
    FOREIGN KEY (WAREHOUSE_ID)
    REFERENCES WAREHOUSES (ID),
    UNIQUE(MANAGER, EMPLOYEE)
);

MERGE INTO WAREHOUSES(ID, NAME, TIMEZONE, CODE)
    KEY (ID)
    VALUES (1, 'SOF', 'Europe/Moscow', 172);
