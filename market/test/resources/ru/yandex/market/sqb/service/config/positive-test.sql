 select cast(SYS_GEN_1.id as NUMBER(10)) as FEED_ID, cast(SYS_GEN_2.datasource_id as NUMBER(10)) as SHOP_ID, cast(SYS_GEN_3.url as varchar2(1024)) as URL, (nvl(12345, 0)) as CONSTANT, cast((SYS_GEN_5.value) as NUMBER(1)) as IS_ENABLED
        from shops_web.datasource main_datasource
        inner join shops_web.datafeed main_datafeed on main_datafeed.datasource_id = main_datasource.id
        left outer join (
select id, id df_id from shops_web.datafeed
) SYS_GEN_1 on SYS_GEN_1.df_id = main_datafeed.id
left outer join (
select datasource_id, id df_id from shops_web.datafeed
) SYS_GEN_2 on SYS_GEN_2.df_id = main_datafeed.id
left outer join (
select url, id df_id from shops_web.datafeed
) SYS_GEN_3 on SYS_GEN_3.df_id = main_datafeed.id
left outer join (
select 1 as value, 1 as datafeed_id from dual
) SYS_GEN_5 on SYS_GEN_5.datafeed_id = main_datafeed.id
order by FEED_ID DESC
