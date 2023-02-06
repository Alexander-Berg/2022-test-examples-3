create table if not exists message
(
    id int unsigned auto_increment
        primary key,
    user_id int not null,
    shop_id int null,
    title varchar(255) not null,
    announcement varchar(255) null,
    text longtext not null,
    datetime datetime not null,
    status varchar(20) default 'NEW' not null,
    type varchar(20) default 'DEFAULT' not null,
    date_read datetime null,
    tag varchar(255) null
--    constraint FK_B6BD307F4D16C4DD
--        foreign key (shop_id) references shop (id)
--            on delete cascade,
--    constraint FK_B6BD307FA76ED395
--        foreign key (user_id) references user (id)
--            on delete cascade
);

create index IDX_B6BD307F4D16C4DD
    on message (shop_id);

create index IDX_B6BD307FA76ED395
    on message (user_id);




create table if not exists command_list
(
    id int auto_increment
        primary key,
    shop_id int null,
    feed_id int null,
    command varchar(255)  not null,
    start_after datetime default CURRENT_TIMESTAMP null,
    start_at datetime null,
    finish_at datetime null,
    is_run tinyint(1) not null,
    error longtext  not null,
    is_success tinyint(1) not null,
    created_at datetime not null,
    args longtext  not null comment '(DC2Type:array)',
    output longtext  null,
    options longtext  not null comment '(DC2Type:array)',
    name varchar(255)  null,
    last_result longtext null comment '(DC2Type:object)',
    updated_at datetime null,
    runtime_db int null,
    runtime_api int null,
    runtime_other int null,
    priority smallint unsigned default 5 not null,
    data longtext null comment '(DC2Type:json_array)',
    worker_type enum('php', 'java') default 'php' null
--     constraint FK_E56256954D16C4DD_1
--         foreign key (shop_id) references shop (id),
--     constraint fk_command_shop_feed_1
--         foreign key (feed_id) references shop_feed (id)
--             on update cascade on delete cascade
);

create index IDX_E56256954D16C4DD
    on command_list (shop_id);

create index check_idx
    on command_list (shop_id, name, created_at, start_at);

create index command_idx
    on command_list (command);

create index is_run_idx
    on command_list (is_run);

create index is_success_idx
    on command_list (is_success);

create index name_idx
    on command_list (name);

create index priority_created_at_idx
    on command_list (priority, created_at);

create index shop_name_finish_idx
    on command_list (shop_id, name, finish_at);

create index start_at_created_at
    on command_list (start_at, created_at);

