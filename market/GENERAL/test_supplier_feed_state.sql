--liquibase formatted sql

--changeset nastik:MBI-29181-create-tst-supplier-feed-state
create table shops_web.test_supplier_feed_state (
   fid number not null,
   gid number not null,
	 return_code number(1,0) not null,
   published_session varchar2(20),
   release_time date,
   noffers number,
   total_offers number
);

--changeset nastik:MBI-27819-pk
alter table shops_web.test_supplier_feed_state
add constraint tst_sup_feed_state_pk primary key (fid);

--changeset nastik:MBI-29181-comments
comment on table shops_web.test_supplier_feed_state is 'Состояние фидов поставщиков по результатам тестовой индексации';
comment on column shops_web.test_supplier_feed_state.gid is 'Идентификатор тестового поколения';
comment on column shops_web.test_supplier_feed_state.fid is 'Идентификатор фида';
comment on column shops_web.test_supplier_feed_state.return_code is 'Результат последней тестовой индексации фида';
comment on column shops_web.test_supplier_feed_state.published_session is 'Id сессии плейншифта, в которой фид был опубликован';
comment on column shops_web.test_supplier_feed_state.release_time is 'Время публикации тестового индекса';
comment on column shops_web.test_supplier_feed_state.noffers is 'Количество распаршенных предложений в фиде';
comment on column shops_web.test_supplier_feed_state.total_offers is 'Общее количество предложений в фиде';


--changeset nastik:MBI-27819-fk-fid
alter table shops_web.test_supplier_feed_state
add constraint tst_sup_feed_state_fid_fk foreign key (fid) references shops_web.supplier_feed (id);

--changeset sviperll:MBI-34009-fk-fid
alter table shops_web.test_supplier_feed_state
drop constraint tst_sup_feed_state_fid_fk;

--changeset nastik:MBI-40906-add-feed-process-type-for-datacamp
alter table shops_web.test_supplier_feed_state add feed_processing_type varchar2(10 char);
comment on column shops_web.test_supplier_feed_state.feed_processing_type is 'Тип обработки фида партера, схема работы';

--changeset sherafgan:MBI-60796-add-business-id-2
alter table shops_web.test_supplier_feed_state add business_id number;
comment on column shops_web.test_supplier_feed_state.business_id is 'Под каким бизнесом проиндексировался фид поставщика';

--changeset sherafgan:MBI-62230-add-is-united-catalog
alter table shops_web.test_supplier_feed_state add is_united_catalog number (1) default null;
comment on column shops_web.test_supplier_feed_state.is_united_catalog is 'Проиндексировался ли партнер с Единым каталогом';
