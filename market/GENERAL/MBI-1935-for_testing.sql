CREATE TABLE REGIONS_ALIAS
(
  REGION_ID  NUMBER,
  NAME       VARCHAR2(100 BYTE)
)
/
CREATE SYNONYM MBI_BILLING.REGIONS_ALIAS FOR REGIONS_ALIAS
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (10231, 'республика Алтай')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (1, 'Московская область')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (11193, 'Ханты-Мансийский автономный округ')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (11111, 'республика Башкортостан')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (11148, 'республика Удмуртская')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (11119, 'республика Татарстан')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (10672, 'Воронежская область')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (10174, 'Ленинградская область')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (10658, 'Владимирская область')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (11156, 'республика Чувашская')
/
Insert into SHOPS_WEB.REGIONS_ALIAS
   (REGION_ID, NAME)
 Values
   (11443, 'республика Саха')
/   
