/*
        inserter.process(new Shop(155l, new Settings("http://aida:39003/shop/ozon", "hello1", "URL", "XML")));
        inserter.process(new Shop(211l, new Settings("http://aida:39003/shop/mvideo", "hello2", "URL", "JSON")));
        inserter.process(new Shop(62666l, new Settings("http://aida:39003/shop/lamoda", "hello3", "HEADER", "XML")));
        inserter.process(new Shop(3828l, new Settings("http://aida:39003/shop/svyaznoy", "hello4", "HEADER", "JSON")));

    //60
    CPA_DESIRE(ValueType.BOOLEAN, "Желание магазина размещаться по CPA", EntityName.DATASOURCE),
    //66
    CPA_SHOP_CURRENCY(ValueType.STRING, "Валюта магазина", EntityName.DATASOURCE),
    //65
    CPA_FIRST_DESIRE(ValueType.DATE, "Дата, когда магазин первый раз пожелал попробовать CPA", EntityName.DATASOURCE, true),
    //61
    CPA_PUSH_URL(ValueType.STRING, "URL, по которому будет ходить PUSH-API в магазин", EntityName.DATASOURCE),
    //62
    CPA_PUSH_TOKEN(ValueType.STRING, "Авторизационный токен магазина для PUSH-API", EntityName.DATASOURCE),
    //63
    CPA_PUSH_AUTH_TYPE(ValueType.STRING, "Тип авторизации для PUSH-API", EntityName.DATASOURCE),
    //64
    CPA_PUSH_FORMAT(ValueType.STRING, "Формат данных, используемый в PUSH-API магазина", EntityName.DATASOURCE),

*/
/
SET DEFINE OFF;
/
delete from SHOPS_WEB.param_value where entity_id = 155 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','155','1','1',null,null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'65','155','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','155','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'61','155','1',null,'http://aida:39003/shop/ozon',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'62','155','1',null,'hello1',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','155','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','155','1',null,'XML',null)
/
--inserter.process(new Shop(211l, new Settings("http://aida:39003/shop/mvideo", "hello2", "URL", "JSON")));
/
/
delete from SHOPS_WEB.param_value where entity_id = 211 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','211','1','1',null,null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'65','211','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','211','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'61','211','1',null,'http://aida:39003/shop/mvideo',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'62','211','1',null,'hello2',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','211','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','211','1',null,'JSON',null)
/
--inserter.process(new Shop(62666l, new Settings("http://aida:39003/shop/lamoda", "hello3", "HEADER", "XML")));
/
/
delete from SHOPS_WEB.param_value where entity_id = 62666 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','62666','1','1',null,null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'65','62666','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','62666','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'61','62666','1',null,'http://aida:39003/shop/lamoda',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'62','62666','1',null,'hello3',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','62666','1',null,'HEADER',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','62666','1',null,'XML',null)
/
--inserter.process(new Shop(82518l, new Settings("http://market-test-push-api.yandex.net:4322", "testShopApiToken", "URL", "XML")));
/
delete from SHOPS_WEB.param_value where entity_id = 82518 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','82518','1','1',null,null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'65','82518','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','82518','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'61','82518','1',null,'http://market-test-push-api.yandex.net:4322',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'62','82518','1',null,'testShopApiToken',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','82518','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','82518','1',null,'XML',null)
/
--inserter.process(new Shop(82557l, new Settings("http://market-test-push-api.yandex.net:4321", "testShopApiToken", "HEADER", "JSON")));
/
delete from SHOPS_WEB.param_value where entity_id = 82557 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','82557','1','1',null,null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'65','82557','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','82557','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'61','82557','1',null,'http://market-test-push-api.yandex.net:4321',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'62','82557','1',null,'testShopApiToken',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','82557','1',null,'HEADER',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','82557','1',null,'JSON',null)
/
 --inserter.process(new Shop(774l, new Settings("http://aida.yandex.ru:39003/shop/774/", "testToken774", "URL", "JSON")));
/
delete from SHOPS_WEB.param_value where entity_id = 774 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','774','1','1',null,null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'65','774','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','774','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'61','774','1',null,'http://aida.yandex.ru:39003/shop/774/',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'62','774','1',null,'testToken774',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','774','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value (PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','774','1',null,'JSON',null)
/
--inserter.process(new Shop(86358l, new Settings("http://aida.yandex.ru:39003/shop/86358/", "testToken774", "URL", "JSON")));
/
delete from SHOPS_WEB.param_value where entity_id = 86358 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','86358','1','1',null,null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'65','86358','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','86358','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'61','86358','1',null,'http://aida.yandex.ru:39003/shop/86358/',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'62','86358','1',null,'testToken774',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','86358','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','86358','1',null,'JSON',null)
/
--inserter.process(new Shop(35201l, new Settings("http://aida.yandex.ru:39003/shop/774/", "testToken774", "URL", "JSON")));
/
delete from SHOPS_WEB.param_value where entity_id = 35201 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','35201','1','1',null,null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'65','35201','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','35201','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'61','35201','1',null,'http://aida.yandex.ru:39003/shop/35201/',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'62','35201','1',null,'testToken774',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','35201','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','35201','1',null,'JSON',null)
/
--inserter.process(new Shop(3828l, new Settings("http://aida.yandex.ru:39003/shop/3828/", "testToken774", "URL", "JSON")));
/
delete from SHOPS_WEB.param_value where entity_id = 3828 and PARAM_TYPE_ID in (60,61,62,63,64,65,66)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'60','3828','1','1',null,null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'65','3828','1',null,null,to_date('04.07.13 18:27:01','DD.MM.YY HH24:MI:SS'));
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'66','3828','1',null,'RUR',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'61','3828','1',null,'http://aida.yandex.ru:39003/shop/3828/',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values
(shops_web.s_param_value.nextval,'62','3828','1',null,'testToken774',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'63','3828','1',null,'URL',null)
/
Insert into SHOPS_WEB.param_value
(PARAM_VALUE_ID,PARAM_TYPE_ID,ENTITY_ID,NUM,NUM_VALUE,STR_VALUE,DATE_VALUE)
values (shops_web.s_param_value.nextval,'64','3828','1',null,'JSON',null)
/



