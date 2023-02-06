package ru.yandex.physics;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RequestExecuterTest {

    private static String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

    @Test
    public void checkTaxpayerStatusTest() {
        RequestExecuter executer = new RequestExecuter();
        Assertions.assertEquals(executer.checkTaxpayerStatus(null, currentDate), TaxpayerStatus.INVALID_INN);
        Assertions.assertEquals(executer.checkTaxpayerStatus("590425291300", currentDate), TaxpayerStatus.OK);
        Assertions.assertEquals(executer.checkTaxpayerStatus("03-362231322", currentDate), TaxpayerStatus.INVALID_INN);
        Assertions.assertEquals(executer.checkTaxpayerStatus("781143073873", currentDate), TaxpayerStatus.OK);
    }
}
