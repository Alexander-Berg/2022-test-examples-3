package ru.yandex.travel.orders.services;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import ru.yandex.misc.test.Assert;
import ru.yandex.travel.orders.entities.StocksResponse;

@Slf4j
public class ExchangeRateServiceTest {

    @Test
    public void testCorrectSerialization() throws IOException {
        String response = "{\"stocks\":[{\"id\":4022,\"bda2\":\"0.00\",\"bdr0\":\"0\",\"df\":-1,\"bv\":\"5.1300\",\"bdr\":\"-0.05\",\"bda\":\"-0.0024\",\"bv0\":\"5\",\"bv2\":\"5.13\",\"uc\":false,\"bda0\":\"0\",\"ut\":1547812800,\"ishot\":\"0\",\"geo\":10000,\"bdr2\":\"0.00\",\"dt\":\"2019-01-18 15:00\"},{\"id\":2002,\"bda2\":\"0.17\",\"bdr0\":\"2\",\"df\":1,\"bv\":\"66.5600\",\"bdr\":\"0.26\",\"bda\":\"0.1725\",\"bv0\":\"67\",\"search_app_graph_base_url\":\"https://avatars.mdst.yandex.net/get-stocks/4172/767accf8-21a8-490e-beb5-57b035c868bc_2002.png/\",\"bv2\":\"66.56\",\"uc\":false,\"bda0\":\"1\",\"history\":[\"66.5600\",\"66.3875\",\"66.2750\",\"66.3700\",\"66.3675\",\"66.9600\",\"67.0750\"],\"ut\":1548148858,\"ishot\":\"1\",\"geo\":10000,\"bdr2\":\"0.26\",\"dt\":\"2019-01-22 12:20\"},{\"id\":2002,\"bda2\":\"0.11\",\"bdr0\":\"0\",\"df\":1,\"bv\":\"66.3875\",\"bdr\":\"0.17\",\"bda\":\"0.1125\",\"bv0\":\"66\",\"search_app_graph_base_url\":\"https://avatars.mdst.yandex.net/get-stocks/4172/767accf8-21a8-490e-beb5-57b035c868bc_2002.png/\",\"bv2\":\"66.39\",\"uc\":false,\"bda0\":\"0\",\"ut\":1548103439,\"ishot\":\"0\",\"geo\":10000,\"bdr2\":\"0.17\",\"dt\":\"2019-01-21 23:43\"},{\"id\":2002,\"bda2\":\"-0.09\",\"bdr0\":\"0\",\"df\":-1,\"bv\":\"66.2750\",\"bdr\":\"-0.14\",\"bda\":\"-0.0950\",\"bv0\":\"66\",\"search_app_graph_base_url\":\"https://avatars.mdst.yandex.net/get-stocks/4172/767accf8-21a8-490e-beb5-57b035c868bc_2002.png/\",\"bv2\":\"66.28\",\"uc\":false,\"bda0\":\"0\",\"ut\":1547844596,\"ishot\":\"0\",\"geo\":10000,\"bdr2\":\"-0.14\",\"dt\":\"2019-01-18 23:49\"},{\"id\":2002,\"bda2\":\"0.00\",\"bdr0\":\"0\",\"df\":1,\"bv\":\"66.3700\",\"bdr\":\"0.00\",\"bda\":\"0.0025\",\"bv0\":\"66\",\"search_app_graph_base_url\":\"https://avatars.mdst.yandex.net/get-stocks/4172/767accf8-21a8-490e-beb5-57b035c868bc_2002.png/\",\"bv2\":\"66.37\",\"uc\":false,\"bda0\":\"0\",\"ut\":1547758198,\"ishot\":\"0\",\"geo\":10000,\"bdr2\":\"0.00\",\"dt\":\"2019-01-17 23:49\"},{\"id\":2002,\"bda2\":\"-0.59\",\"bdr0\":\"-1\",\"df\":-1,\"bv\":\"66.3675\",\"bdr\":\"-0.88\",\"bda\":\"-0.5925\",\"bv0\":\"66\",\"search_app_graph_base_url\":\"https://avatars.mdst.yandex.net/get-stocks/4172/767accf8-21a8-490e-beb5-57b035c868bc_2002.png/\",\"bv2\":\"66.37\",\"uc\":false,\"bda0\":\"-1\",\"ut\":1547671797,\"ishot\":\"0\",\"geo\":10000,\"bdr2\":\"-0.88\",\"dt\":\"2019-01-16 23:49\"}]}";
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        StocksResponse stocksResponse = objectMapper.readValue(response, StocksResponse.class);
        Assert.equals(stocksResponse.getStocks().size(), 6);
    }

}
