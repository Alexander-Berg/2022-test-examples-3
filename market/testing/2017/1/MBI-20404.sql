--liquibase formatted sql

--changeset fbokovikov:MBI-20404-testing-delivery-service-1
merge into SHOPS_WEB.DELIVERY_SERVICES dst
using (select 774 id, 'TestTrack' name, 'www.testtrack.test.com' url, 'TestTrack' HUMAN_READABLE_ID, 0 PICKUP_AVAILABLE, 1 IS_GLOBAL, 1 IS_COMMON, 'carrier' TYPE from dual) src
on (dst.id = src.id)
when not matched then insert (ID, NAME, URL, HUMAN_READABLE_ID, PICKUP_AVAILABLE, IS_GLOBAL, IS_COMMON, TYPE)
values (src.ID, src.NAME, src.URL, src.HUMAN_READABLE_ID, src.PICKUP_AVAILABLE, src.IS_GLOBAL, src.IS_COMMON, src.TYPE)
;