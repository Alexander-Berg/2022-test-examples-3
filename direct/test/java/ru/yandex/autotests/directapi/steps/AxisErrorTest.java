package ru.yandex.autotests.directapi.steps;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.directapi.apiclient.errors.AxisError;
import ru.yandex.autotests.directapi.apiclient.errors.AxisErrorDetails;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: mariabye
 * Date: 24.08.12
 * Time: 12:26
 * To change this template use File | Settings | File Templates.
 */
public class AxisErrorTest{
    Logger log = LogManager.getLogger(this.getClass());

    @Ignore
    @Test
    public void latinErrorMatchTest(){
        AxisError error = new AxisError(71, AxisErrorDetails.IN_BANNER_PRICE_FIELD_VALUE,213456);
        log.info(error);
        
    }

    @Ignore
    @Test
    public void cyrilicErrorMatchTest(){
        AxisError error = new AxisError(71, AxisErrorDetails.LIGHT_NOT_ALLOWED_MANAGE_CAMPAIGNS);
        log.info(error);

    }

    @Ignore
    @Test
    public void errorTest() throws UnsupportedEncodingException {
        AxisError error = new AxisError(71, AxisErrorDetails.TEST);
        log.info(error);
        assertThat(error.getDetails(), equalTo("АБВАБВ"));
    }

}
