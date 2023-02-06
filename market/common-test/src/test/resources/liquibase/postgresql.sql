--liquibase formatted sql

--changeset s-ermakov:postgres
create schema if not exists common_test;

-- для тестов приниципиально важно, чтобы один тип создался в схеме, другой в схеме по-умолчанию
create type common_test.number_type as enum ('INT', 'DOUBLE');
create type letter_type as enum ('CAPITAL', 'LOWERCASE');

create table common_test.numbers
(
    number      int not null,
    number_type common_test.number_type
);
create table common_test.letters
(
    letter      text not null,
    letter_type letter_type
);

create materialized view common_test.number_letters as
select number || letter as number_letter
from common_test.letters,
     common_test.numbers;
