--liquibase formatted sql

--changeset nastik:MBI-17919 endDelimiter:endDelimiter:///
DECLARE
 PROCEDURE ADD_CATEGORY(hyperId NUMBER, cpaType NUMBER, fee NUMBER, regionIdArray shops_web.t_number_tbl) IS
    BEGIN
  MERGE INTO MARKET_BILLING.CPA_CATEGORIES t
  USING (SELECT
           hyperId             hyper_id,
           cpaType    cpa_type,
           fee             fee
         FROM dual) n ON (n.hyper_id = t.hyper_id and n.cpa_type = t.cpa_type)
  WHEN MATCHED THEN
  UPDATE SET t.fee = n.fee
  WHEN NOT MATCHED THEN
  INSERT (hyper_id, cpa_type, fee) VALUES (n.hyper_id, n.cpa_type, n.fee);

  MERGE INTO MARKET_BILLING.CPA_CATEGORY_REGIONS tar
    USING (
            select hyperId hyper_id,
            value(ids) region_id from table(regionIdArray)ids
          ) src
    ON (tar.hyper_id = src.hyper_id and tar.region_id = src.region_id)
  WHEN NOT MATCHED THEN
  INSERT (hyper_id, region_id) VALUES (src.hyper_id, src.region_id);
END ADD_CATEGORY;

BEGIN
ADD_CATEGORY(90478, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(308016, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90404, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90417, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(6269371, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90462, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(4317343, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90565, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90575, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90578, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90577, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(734595, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(477439, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(237418, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90580, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90581, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(237420, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90584, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90594, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(765280, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90601, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90590, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90588, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90592, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90595, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90591, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90600, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(411498, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(13351779, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90602, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90587, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(411499, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90589, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90598, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90599, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(1564516, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(1005910, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90564, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90566, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90568, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90582, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(242704, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(278373, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90567, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(7683675, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(278374, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90569, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90570, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91161, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(191219, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91465, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(10683251, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(6159024, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(10477020, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(12410815, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(10470548, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(989027, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(989023, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(989031, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(512743, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(989025, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(10683243, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(454909, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90720, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(267388, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(281935, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(267389, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(267390, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(1003092, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(278423, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91657, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91650, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(763072, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91651, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91661, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91662, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(12385944, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91611, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91664, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91616, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(2190938, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91618, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91614, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91610, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91612, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(284394, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(226666, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(226667, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90950, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90855, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91248, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91244, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91076, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91082, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(857707, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91031, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91033, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91027, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91028, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(818965, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91020, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(191211, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91035, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91019, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91013, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(6427100, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91032, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(288003, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(138608, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91112, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(4684840, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(723088, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91107, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(6368403, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91052, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91117, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91088, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91029, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(723087, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91095, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(979262, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(543487, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(281429, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91521, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(477533, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(396898, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(396900, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91519, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91522, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91520, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90548, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(542020, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90544, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90561, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90554, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90549, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90636, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(4165204, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90639, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90633, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91105, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91122, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90559, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90545, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90546, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90560, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(2417247, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(8353924, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91491, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91463, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91464, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91470, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90616, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90635, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90613, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90617, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91148, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(1596792, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(90619, 3, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(294661, 3, 200, shops_web.t_number_tbl(213));
END;
///