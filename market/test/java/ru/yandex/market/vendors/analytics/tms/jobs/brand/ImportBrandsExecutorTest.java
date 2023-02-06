package ru.yandex.market.vendors.analytics.tms.jobs.brand;

import java.nio.charset.StandardCharsets;

import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.util.StringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.brand.BrandInfo;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ImportBrandsExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "SaveBrandInfosTest.before.csv")
public class ImportBrandsExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<BrandInfo> ytBrandReader;
    @Autowired
    private YtTableReader<BrandInfo> ytFarmaVendorsReader;
    @Autowired
    private ImportBrandsExecutor importBrandsExecutor;

    @Test
    @DbUnitDataSet(after = "SaveBrandInfosTest.after.csv")
    void importBrandsExecutorTest() {
        //noinspection unchecked
        reset(ytBrandReader, ytFarmaVendorsReader);
        when(ytBrandReader.loadInfoFromYtTableWithEncoding()).thenReturn(
                ImmutableList.of(
                        new BrandInfo(1L, encodeString("Самсунг")),
                        new BrandInfo(2L, encodeString("Яблоко")),
                        new BrandInfo(3L, encodeString("Груша")),
                        new BrandInfo(4L, encodeString("GMC")),
                        new BrandInfo(5L, encodeString("Василий Пупкин")),
                        new BrandInfo(6L, encodeString("НаСамомДелеФарма"))
                )
        );
        when(ytFarmaVendorsReader.loadInfoFromYtTableWithEncoding()).thenReturn(
                ImmutableList.of(
                        new BrandInfo(101L, encodeString("Виагра")),
                        new BrandInfo(102L, encodeString("НаСамомДелеНЕФарма"))
                )
        );
        importBrandsExecutor.doJob(null);
    }

    private String encodeString(String name) {
        return new String(StringUtil.getUtf8Bytes(name), StandardCharsets.ISO_8859_1);
    }
}
