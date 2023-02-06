package ru.yandex.market.mboc.common.services.mstat.yt;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * @author apluhin
 * @created 2/18/21
 */
public class MstatOffersTableLoaderTest {

    @Test
    public void extra() throws JsonProcessingException {
        Map<String, String> a = Map.of("1", "2");
        ObjectMapper m = new ObjectMapper();
        System.out.println(m.writeValueAsString(a));
    }
}
