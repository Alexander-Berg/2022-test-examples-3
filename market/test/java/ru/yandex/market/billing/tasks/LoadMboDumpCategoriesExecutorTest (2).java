package ru.yandex.market.billing.tasks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.googlecode.protobuf.format.JsonFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.Magics;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.shop.FunctionalTest;

class LoadMboDumpCategoriesExecutorTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Module sourceModule;

    @Autowired
    private EnvironmentService environmentService;

    @DisplayName("Проверка заполнения базы категорий из МБО")
    @Test
    @DbUnitDataSet(before = "loadMboDumpCategoriesExecutorTest.before.csv",
            after = "loadMboDumpCategoriesExecutorTest.after.csv")
    void loadCategories() throws IOException {
        String input = StringTestUtil.getString(this.getClass(), "loadMboDumpCategoriesExecutorTest.stub.json");
        MboParameters.Categories.Builder builder = MboParameters.Categories.newBuilder();
        JsonFormat.merge(input, builder);
        List<MboParameters.Category> categories = builder.build().getCategoryList();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Magics.MagicConstants.MBOC.name().getBytes());
        categories.forEach(cat -> safeWrite(cat, baos));

        LoadMboDumpCategoriesExecutor executor =
                new LoadMboDumpCategoriesExecutor(jdbcTemplate,
                        transactionTemplate,
                        sourceModule,
                        environmentService
                        , null);
        executor.doImport(new ByteArrayInputStream(baos.toByteArray()));
    }

    private void safeWrite(final MboParameters.Category category, ByteArrayOutputStream baos) {
        try {
            category.writeDelimitedTo(baos);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
