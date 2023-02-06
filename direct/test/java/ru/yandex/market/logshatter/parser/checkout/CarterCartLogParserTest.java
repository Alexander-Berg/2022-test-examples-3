package ru.yandex.market.logshatter.parser.checkout;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

public class CarterCartLogParserTest {

    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new CarterCartLogParser());
    }

    @Test
    public void testRecordWithAllFields() throws Exception {
        checker.check("tskv\tevent_time=2017-06-28T11:07:48+0300\tevent_type=CREATE\tuser_id=101" +
                        "\tuser_id_type=YANDEXUID\tlist_id=8928741\titem_id=410011\titem_type=OFFER\titem_count=1" +
                        "\tware_md5=objId001\tmodel_id=777\thid=12345\tshop_id=123\tprice=1E+3\tfee_sum=2.27\n",
                new Date(1498637268000L), checker.getHost(), CartLogActionType.CREATE, "101", UserIdType.YANDEXUID,
                8928741L, 410011L, CartItemType.OFFER, 1, "objId001", "777", "12345", "123", 1000.0, 2.27);
    }

    @Test
    public void tesRecordWithOutOptionalFields() throws Exception {
        checker.check("tskv\tevent_time=2017-06-28T11:10:53+0300\tevent_type=DELETE\tuser_id=486813774" +
                        "\tuser_id_type=UID\tlist_id=8928721\titem_id=410012\titem_type=OFFER\titem_count=3" +
                        "\tware_md5=objId003\n",
                new Date(1498637453000L), checker.getHost(), CartLogActionType.DELETE, "486813774", UserIdType.UID,
                8928721L, 410012L, CartItemType.OFFER, 3, "objId003", "", "", "", Double.NaN, Double.NaN);
    }

}
