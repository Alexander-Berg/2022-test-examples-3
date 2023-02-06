package ru.yandex.direct.grid.processing.service.showcondition.tools;

import java.util.List;

import org.jooq.Select;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionOrderBy;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionOrderByField;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.PHRASESTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

public class ShowConditionCommonUtils {
    public static final GdShowConditionOrderBy ORDER_BY_ID = new GdShowConditionOrderBy()
            .withField(GdShowConditionOrderByField.ID)
            .withOrder(Order.ASC);
    private static final int MILLION = 1_000_000;

    private ShowConditionCommonUtils() {
    }

    public static Answer<UnversionedRowset> getAnswer(List<AdGroupInfo> groups, List<KeywordInfo> keywords) {
        return invocation -> {
            Select query = invocation.getArgument(1);
            if (query.toString().contains(BIDSTABLE_DIRECT.getName())) {
                return convertToKeywordNode(keywords);
            }
            return convertToGroupRowset(groups);
        };
    }

    private static UnversionedRowset convertToKeywordNode(List<KeywordInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(BIDSTABLE_DIRECT.ID.getName(), info.getId())
                        .withColValue(BIDSTABLE_DIRECT.PRICE.getName(),
                                info.getKeyword().getPrice().longValueExact() * MILLION)
                        .withColValue(BIDSTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(BIDSTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(BIDSTABLE_DIRECT.PHRASE_ID.getName(), info.getId())
                        .withColValue(BIDSTABLE_DIRECT.PHRASE.getName(), info.getKeyword().getPhrase())
                        .withColValue(BIDSTABLE_DIRECT.IS_SUSPENDED.getName(), info.getKeyword().getIsSuspended())
                        .withColValue(BIDSTABLE_DIRECT.IS_DELETED.getName(), booleanToLong(false))
                        .withColValue(BIDSTABLE_DIRECT.BID_TYPE.getName(),
                                GdiShowConditionType.KEYWORD.name().toLowerCase())
                        .withColValue(BIDSTABLE_DIRECT.IS_ARCHIVED.getName(), booleanToLong(false))
        ));

        return builder.build();
    }

    private static UnversionedRowset convertToGroupRowset(List<AdGroupInfo> infos) {
        RowsetBuilder builder = rowsetBuilder();
        infos.forEach(info -> builder.add(
                rowBuilder()
                        .withColValue(PHRASESTABLE_DIRECT.PID.getName(), info.getAdGroupId())
                        .withColValue(PHRASESTABLE_DIRECT.CID.getName(), info.getCampaignId())
                        .withColValue(PHRASESTABLE_DIRECT.ADGROUP_TYPE.getName(),
                                info.getAdGroupType().name().toLowerCase())
        ));

        return builder.build();
    }

}
