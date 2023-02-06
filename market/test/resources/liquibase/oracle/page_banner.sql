--liquibase formatted sql

--changeset s-myachenkov:test-ora
create table shops_web.page_banner (
       page_id varchar(256) not null,
       banner_id varchar(256) not null,
       page_only smallint default 0
) ;
comment on column shops_web.page_banner.page_only is e'маппинг по поставщикам отсуствует - для всех поставщиков по данной странице';
comment on column shops_web.page_banner.page_id is e'название страницы';
create index i_pagban_pagid_banid on shops_web.page_banner (page_id, banner_id);
create index i_pagban_banid on shops_web.page_banner (banner_id);
