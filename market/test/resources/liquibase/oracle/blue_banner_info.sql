--liquibase formatted sql
--changeset stani:test-ora
create table shops_web.blue_banner_info (
                                            id varchar(256) not null,
                                            is_permanent smallint not null,
                                            img varchar(1024),
                                            text varchar(1024) not null,
                                            button varchar(1024),
                                            url varchar(1024),
                                            background_color_hex varchar(10),
                                            severity numeric
) ;
create index i_blubaninf_id on shops_web.blue_banner_info (id);
