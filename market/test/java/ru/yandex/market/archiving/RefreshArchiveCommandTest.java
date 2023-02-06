package ru.yandex.market.archiving;

import java.io.PrintWriter;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link RefreshArchiveCommand}.
 *
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 */
class RefreshArchiveCommandTest extends FunctionalTest {

    private static final String COMMAND_NAME = "refresh-archive";

    //language=sql
    private static final String INSERT_BEFORE_TEST = "insert into " +
            "shops_web.datasource_archiving(partner_id, data, archiving_date)" +
            "values(:partner_id, :data, :archiving_date)";

    //language=sql
    private static final String SELECT_AFTER_REFRESHING = "select * " +
            "from shops_web.datasource_archiving " +
            "where partner_id = :partner_id";

    @Autowired
    private RefreshArchiveCommand refreshArchiveCommand;

    @Autowired
    private Terminal terminal;

    @Autowired
    private PrintWriter printWriter;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static Stream<Arguments> wrongArgumentsForArchiveRefresh() {
        return Stream.of(
                Arguments.of(commandInvocation(new String[]{"argument1", "argument2"})),
                Arguments.of(commandInvocation(new String[]{""})),
                Arguments.of(commandInvocation(new String[]{StringTestUtil.getString(RefreshArchiveCommandTest.class
                        .getResourceAsStream("base64/refreshArchiveCommand/refresh_archive_data_wrong_base_64"))}))
        );
    }

    private static CommandInvocation commandInvocation(String[] arguments) {
        return new CommandInvocation(COMMAND_NAME, arguments, Collections.emptyMap());
    }

    @Test
    @DisplayName("Обновление заархивированных данных")
    @DbUnitDataSet(before = "csv/refreshArchiveCommand/refresh.before.csv")
    void testRefreshArchiveData() {
        beforeRefresh();
        when(terminal.getWriter()).thenReturn(printWriter);

        String dataForRefresh = StringTestUtil.getString(getClass()
                .getResourceAsStream("base64/refreshArchiveCommand/refresh_archive_data_base_64"));

        refreshArchiveCommand.executeCommand(new CommandInvocation(COMMAND_NAME,
                new String[]{dataForRefresh}, Collections.emptyMap()), terminal);

        checkResult();
    }

    @DisplayName("Проверить, что при передаче данных в неправильном формате будет бросаться исключение")
    @ParameterizedTest(name = "Test for wrong refresh arguments {0}")
    @MethodSource("wrongArgumentsForArchiveRefresh")
    void testRefreshArchiveDataWithWrongArguments(CommandInvocation commandInvocation) {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> refreshArchiveCommand.executeCommand(commandInvocation, terminal));
    }

    private void beforeRefresh() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            namedParameterJdbcTemplate.update(INSERT_BEFORE_TEST, new MapSqlParameterSource()
                    .addValue("partner_id", 1417)
                    .addValue("data", StringTestUtil.getString(getClass()
                            .getResourceAsStream("xml/RefreshArchiveDataBefore.xml")))
                    .addValue("archiving_date", new Date(format.parse("2018-09-06 05:23:47").getTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void checkResult() {
        String expectedResult = StringTestUtil.getString(getClass()
                .getResourceAsStream("xml/RefreshArchiveDataAfter.xml"));

        String actualResult = namedParameterJdbcTemplate.query(SELECT_AFTER_REFRESHING,
                new MapSqlParameterSource().addValue("partner_id", 1417),
                rs -> rs.next()
                        ? rs.getString("data")
                        : "");

        MbiMatchers.xmlEquals(expectedResult, Collections.singleton(actualResult));
    }
}
