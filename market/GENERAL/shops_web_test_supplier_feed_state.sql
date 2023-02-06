--liquibase formatted sql
--changeset jaguar1337:MBI-70471-create-shops-web-test-supplier-feed-state-table
create table shops_web.test_supplier_feed_state (
	fid bigint not null,
	gid numeric not null,
	return_code smallint not null,
	published_session varchar(20),
	release_time timestamp with time zone,
	noffers numeric,
	total_offers numeric,
	feed_processing_type varchar(10),
	business_id bigint,
	is_united_catalog smallint,
	constraint pk_tessupfeesta_fid primary key (fid)
) ;
comment on table shops_web.test_supplier_feed_state is e'состояние фидов поставщиков по результатам тестовой индексации';
comment on column shops_web.test_supplier_feed_state.published_session is e'id сессии плейншифта, в которой фид был опубликован';
comment on column shops_web.test_supplier_feed_state.is_united_catalog is e'проиндексировался ли партнер с единым каталогом';
comment on column shops_web.test_supplier_feed_state.business_id is e'под каким бизнесом проиндексировался фид поставщика';
comment on column shops_web.test_supplier_feed_state.return_code is e'результат последней тестовой индексации фида';
comment on column shops_web.test_supplier_feed_state.fid is e'идентификатор фида';
comment on column shops_web.test_supplier_feed_state.feed_processing_type is e'тип обработки фида партера, схема работы';
comment on column shops_web.test_supplier_feed_state.gid is e'идентификатор тестового поколения';
comment on column shops_web.test_supplier_feed_state.release_time is e'время публикации тестового индекса';
comment on column shops_web.test_supplier_feed_state.noffers is e'количество распаршенных предложений в фиде';
comment on column shops_web.test_supplier_feed_state.total_offers is e'общее количество предложений в фиде';
