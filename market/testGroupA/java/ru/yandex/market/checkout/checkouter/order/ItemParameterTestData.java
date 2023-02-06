package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.common.report.model.filter.Filter;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 * Date: 18/05/2017.
 * @see ItemParameterToPrettyStringTest
 * @see MarketReportFilterValueToItemParameterConverterTest
 */
public class ItemParameterTestData {

    private Filter filter;
    private ItemParameter itemParameter;
    private String prettyString;

    @Nonnull
    private static String getSerializedData() throws IOException, ClassNotFoundException {
        return IOUtils.readInputStream(ItemParameterTestData.class.getResourceAsStream(
                "/files/report/itemParameters.json"));
    }

    public static Collection<ItemParameterTestData> getDataSet() throws IOException, ClassNotFoundException {
        Type listType = new TypeToken<List<ItemParameterTestData>>() {
        }.getType();
        return (new Gson()).<List<ItemParameterTestData>>fromJson(getSerializedData(), listType);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public ItemParameter getItemParameter() {
        return itemParameter;
    }

    public void setItemParameter(ItemParameter itemParameter) {
        this.itemParameter = itemParameter;
    }

    public String getPrettyString() {
        return prettyString;
    }

    public void setPrettyString(String prettyString) {
        this.prettyString = prettyString;
    }

}
