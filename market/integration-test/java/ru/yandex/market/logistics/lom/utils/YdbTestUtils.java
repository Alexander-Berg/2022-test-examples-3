package ru.yandex.market.logistics.lom.utils;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.YdbSelect;
import ru.yandex.market.ydb.integration.utils.ListConverter;

@UtilityClass
@ParametersAreNonnullByDefault
public class YdbTestUtils {

    @Nonnull
    public <T> List<T> findAll(
        YdbTemplate ydbTemplate,
        YdbTableDescription tableDescription,
        ListConverter<T> listConverter
    ) {
        return ydbTemplate.selectList(
            YdbSelect.select(
                    QSelect.of(tableDescription.fields())
                        .from(QFrom.table(tableDescription))
                        .select()
                )
                .toQuery(),
            YdbTemplate.DEFAULT_READ,
            listConverter
        );
    }
}
