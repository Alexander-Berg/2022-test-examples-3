package ru.yandex.market.common.test.spring;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.yandex.market.common.test.jdbc.H2SqlTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Базовый конфиг для тестов, использующих базу H2.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public abstract class H2Config extends DbUnitTestConfig {

    private static final String MY_TRUNC_ALIAS = "" +
            "CREATE ALIAS PUBLIC.my_trunc " +
            "FOR \"ru.yandex.market.common.test.jdbc.DatabaseFunctions.myTrunc\";";
    private static final String XML_TYPE_ALIAS = "" +
            "CREATE ALIAS XMLTYPE FOR \"ru.yandex.market.common.test.jdbc.DatabaseFunctions.toXMLType\";";
    private static final String EXTRACT_VALUE_ALIAS = "" +
            "CREATE ALIAS EXTRACTVALUE FOR \"ru.yandex.market.common.test.jdbc.DatabaseFunctions.extractXMLValue\";";
    private static final String TO_NUMBER_ALIAS = "" +
            "CREATE ALIAS TO_NUMBER FOR \"ru.yandex.market.common.test.jdbc.DatabaseFunctions.toNumber\";";
    private static final String STRAGG_AGGREGATE = "" +
            "CREATE AGGREGATE STRAGG FOR \"ru.yandex.market.common.test.jdbc.functions.StringAggregateFunction\";";

    @Override
    @Nonnull
    protected StringTransformer createSqlTransformer() {
        return new H2SqlTransformer();
    }

    @Override
    @Nonnull
    protected EmbeddedDatabaseType databaseType() {
        return EmbeddedDatabaseType.H2;
    }

    @Nonnull
    @Override
    protected List<Resource> defaultDatabaseResources() {
        final Resource myTrunc = new ByteArrayResource(MY_TRUNC_ALIAS.getBytes(Charset.forName("UTF-8")));
        final Resource xmlType = new ByteArrayResource(XML_TYPE_ALIAS.getBytes(Charset.forName("UTF-8")));
        final Resource extractValue = new ByteArrayResource(EXTRACT_VALUE_ALIAS.getBytes(Charset.forName("UTF-8")));
        final Resource toNumber = new ByteArrayResource(TO_NUMBER_ALIAS.getBytes(Charset.forName("UTF-8")));
        final Resource stragg = new ByteArrayResource(STRAGG_AGGREGATE.getBytes(Charset.forName("UTF-8")));
        return Arrays.asList(myTrunc, xmlType, extractValue, toNumber, stragg);
    }
}
