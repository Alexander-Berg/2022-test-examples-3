package ru.yandex.market.mbo.mdm.common.automarkup;

import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.market.mbo.mdm.common.automarkup.model.MdmAutoMarkupYqlQuery;
import ru.yandex.market.mbo.mdm.common.automarkup.repository.MdmAutoMarkupYqlQueryRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGenericMapperRepositoryTestBase;

public class MdmAutoMarkupYqlQueryRepositoryImplTest
    extends MdmGenericMapperRepositoryTestBase<MdmAutoMarkupYqlQueryRepository, MdmAutoMarkupYqlQuery, Long> {

    @Override
    protected MdmAutoMarkupYqlQuery randomRecord() {
        return random.nextObject(MdmAutoMarkupYqlQuery.class);
    }

    @Override
    protected boolean isIdGenerated() {
        return true;
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[]{};
    }

    @Override
    protected Function<MdmAutoMarkupYqlQuery, Long> getIdSupplier() {
        return MdmAutoMarkupYqlQuery::getId;
    }

    @Override
    protected List<BiConsumer<Integer, MdmAutoMarkupYqlQuery>> getUpdaters() {
        return List.of(
            (i, record) -> record.setName("query_name_" + i),
            (i, record) -> record.setYqlQueryId("yql_query_id_" + i),
            (i, record) -> record.setRowsLimit(10000),
            (i, record) -> record.setUserLogin("test_user"),
            (i, record) -> record.setModifiedAt(Instant.now()),
            (i, record) -> record.setType(i % 2 == 0
                ? MdmAutoMarkupYqlQuery.Type.MSKU_PARAM_YQL
                : MdmAutoMarkupYqlQuery.Type.SSKU_PARAM_YQL)

        );
    }
}
