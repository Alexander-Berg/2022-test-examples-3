package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import java.text.SimpleDateFormat;

public class MediaplanLogParserTest {

    private SimpleDateFormat dateFormat;

    @Test
    public void testParse() throws Exception {
        String line = "<134>1 2019-03-07T07:22:08+03:00 iva1-5915-660-msk-iva-ppc-dire-ede-14646.gencfg-c.yandex.net " +
            "MEDIAPLAN.mediaplan.log 982716 - - 2019-03-07 07:22:08 {\"reqid\":44027688255274396,\"cid\":\"29211331\"," +
            "\"action\":\"cmd_multieditMediaplanEnd\",\"values\":{\"vcard_id\":\"47561261\",\"source_bid\":\"4608296822\"," +
            "\"source_pid\":\"2856818495\",\"phrases\":[{\"ph_id\":\"0\",\"phrase\":\"лента конвейерная 2л\",\"old_id\":\"0\"}]," +
            "\"body\":\"Всегда в наличии. Любая партия. Доставка по РФ. До ТК бесплатно. Консультации 0 ₽.\"," +
            "\"href\":\"https://rosimp-td.ru/catalog/transporternaya-lenta/\",\"title\":\"Купить конвейерную ленту. Недорого\"," +
            "\"geo\":\"26,977,10645,10650,10658,10672,10687,10693,10699,10705,10712,10772,10776,10795,10802,10819,10832,10841\"," +
            "\"extra_data\":{\"phrases_action\":null,\"banner_action\":\"Update\"},\"title_extension\":\"Выгодные цены\"," +
            "\"banner_type\":\"desktop\",\"mbid\":\"10176074\",\"retargetings\":{}}}";

        String values = "{\"vcard_id\":\"47561261\",\"source_bid\":\"4608296822\"," +
            "\"source_pid\":\"2856818495\",\"phrases\":[{\"ph_id\":\"0\",\"phrase\":\"лента конвейерная 2л\",\"old_id\":\"0\"}]," +
            "\"body\":\"Всегда в наличии. Любая партия. Доставка по РФ. До ТК бесплатно. Консультации 0 ₽.\"," +
            "\"href\":\"https://rosimp-td.ru/catalog/transporternaya-lenta/\",\"title\":\"Купить конвейерную ленту. Недорого\"," +
            "\"geo\":\"26,977,10645,10650,10658,10672,10687,10693,10699,10705,10712,10772,10776,10795,10802,10819,10832,10841\"," +
            "\"extra_data\":{\"phrases_action\":null,\"banner_action\":\"Update\"},\"title_extension\":\"Выгодные цены\"," +
            "\"banner_type\":\"desktop\",\"mbid\":\"10176074\",\"retargetings\":{}}";

        LogParserChecker checker = new LogParserChecker(new MediaplanLogParser());

        dateFormat = new SimpleDateFormat(DbShardsIdsLogParser.DATE_PATTERN_PERL);
        checker.check(line,
            dateFormat.parse("2019-03-07T07:22:08+03:00"),
            "MEDIAPLAN.mediaplan.log",
            44027688255274396L,
            "iva1-5915-660-msk-iva-ppc-dire-ede-14646.gencfg-c.yandex.net",
            29211331L,
            "cmd_multieditMediaplanEnd",
            values
        );
    }
}