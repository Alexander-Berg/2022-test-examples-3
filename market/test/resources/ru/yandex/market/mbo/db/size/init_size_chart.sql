SET MODE Oracle;

create table size_chart
(
    id         NUMBER             not null,
    option_id  NUMBER,
    chart_name VARCHAR2(270 char) not null,
    vendor_id  NUMBER
);

create table size_chart_measure
(
    id                NUMBER not null,
    size_id           NUMBER not null,
    size_measure_id   NUMBER not null,
    option_id         NUMBER not null,
    min_value         NUMBER,
    max_value         NUMBER,
    converted_to_size NUMBER(1),
    used_in_filter    NUMBER(1)
);


create table size_chart_size
(
    id             NUMBER             not null,
    size_chart_id  NUMBER             not null,
    size_option_id NUMBER             not null,
    size_name      VARCHAR2(270 char) not null
);


create table size_chart_size_category
(
    size_id     NUMBER,
    category_id NUMBER,
    remove      NUMBER default 0
);

ALTER TABLE size_chart_measure ADD measure_size_name VARCHAR2(270 char);

CREATE SEQUENCE SIZE_CHART_SEQ;
CREATE SEQUENCE SIZE_CHART_SIZE_SEQ;
CREATE SEQUENCE SIZE_CHART_MEASURE_SEQ;

ALTER TABLE size_chart_measure ADD set_name_by_user NUMBER(1);


create table size_chart_types
(
    parameter_id NUMBER NOT NULL,
    category_id NUMBER NOT NULL,
    option_id NUMBER NOT NULL,
    type varchar(20) NOT NULL
);
