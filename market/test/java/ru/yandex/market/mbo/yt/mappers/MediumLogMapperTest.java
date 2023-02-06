package ru.yandex.market.mbo.yt.mappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.utils.MediumLogUtils;

@RunWith(MockitoJUnitRunner.class)
public class MediumLogMapperTest {
    private MediumLogMapper mapper;
    // Датасеты с прод базы
    private List<Map<String, String>> dataSets;

    @Before
    public void setUp() throws IOException {
        mapper = MediumLogUtils.mapper();
        dataSets = loadDataSets();
    }

    private List<Map<String, String>> loadDataSets() throws IOException {
        InputStream is = new ClassPathResource("/ru/yandex/market/mbo/yt/mappers/md5CheckDataSet.json")
            .getInputStream();

        ObjectMapper om = new ObjectMapper();
        JavaType map = om.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        JavaType javaType = om.getTypeFactory().constructCollectionType(List.class, map);
        return om.readValue(is, javaType);
    }

    @Test
    public void checkMd5() {
        dataSets.forEach(this::checkDataSet);
    }

    private void checkDataSet(Map<String, String> dataSet) {
        YTreeMapNode input = YTree.mapBuilder()
            .key(MediumLogUtils.OFFER).value(dataSet.get(MediumLogUtils.OFFER))
            .key(MediumLogUtils.DESCRIPTION).value(dataSet.get(MediumLogUtils.DESCRIPTION))
            .key(MediumLogUtils.OFFER_PARAMS).value(dataSet.get(MediumLogUtils.OFFER_PARAMS))
            .key(MediumLogUtils.PRICE).value(Double.parseDouble(dataSet.get(MediumLogUtils.PRICE)))

            .key(MediumLogUtils.PARAMS).value(StringUtils.EMPTY)
            .key(MediumLogUtils.BARCODE).value(StringUtils.EMPTY)
            .key(MediumLogUtils.DATASOURCE).value(StringUtils.EMPTY)
            .key(MediumLogUtils.SHOP_CATEGORY_NAME).value(StringUtils.EMPTY)
            .key(MediumLogUtils.SC_MESSAGE).value(StringUtils.EMPTY)
            .key(MediumLogUtils.MARKET_CATEGORY).value(StringUtils.EMPTY)
            .key(MediumLogUtils.PIC_URLS).value(StringUtils.EMPTY)
            .buildMap();

        Yield<YTreeMapNode> yield = Mockito.mock(Yield.class);
        ArgumentCaptor<YTreeMapNode> argumentCaptor = ArgumentCaptor.forClass(YTreeMapNode.class);

        mapper.map(input, yield, null, null);

        Mockito.verify(yield, Mockito.times(1))
            .yield(argumentCaptor.capture());

        YTreeMapNode result = argumentCaptor.getValue();
        Assert.assertEquals(dataSet.get(MediumLogUtils.OFFER_HASH), result.getString(MediumLogUtils.OFFER_HASH));
        Assert.assertEquals(1.0d, result.getDouble(MediumLogUtils.PRICE), 0d);
    }
}
