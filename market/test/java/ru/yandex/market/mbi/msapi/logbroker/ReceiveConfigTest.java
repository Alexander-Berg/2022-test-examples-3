package ru.yandex.market.mbi.msapi.logbroker;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.market.mbi.msapi.logbroker_new.LBUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author kateleb
 */
public class ReceiveConfigTest {

    public static final String CLIENT_ID = "clientId";
    public static final String IDENT = "ident";
    public static final String LOGTYPE = "market-chupakabra-log";
    public static final String IDENT_FROM_TOPIC = "alternative-ident";
    public static final String LOGTYPE_FROM_TOPIC = "market-cuco-log";
    public static final String TOPIC = IDENT_FROM_TOPIC + "/" + LOGTYPE_FROM_TOPIC;
    public static final String USER_LOG_TYPE = "userLogType";
    public static final String RECEIVER = "receiver";

    @Test
    public void testAllParamsSet() {
        ReceiveConfig.Builder builder = new ReceiveConfig.Builder();
        //required
        builder.setClientId(CLIENT_ID);
        builder.setReceiver(RECEIVER);

        //semioptional
        builder.setIdent(IDENT);
        builder.setLogType(LOGTYPE);
        builder.setTopic(TOPIC);

        //optional
        builder.setIsEnabled(true);
        builder.setLogbrokerReadOnlyAfter("2020-02-28T00:00:00");
        builder.setReadOnlyLocal(true);
        builder.setUserLogType(USER_LOG_TYPE);
        builder.setReadTimeout(10);
        builder.setReceiveExecutionTimeLimit(20);
        builder.setReceiveExecutionTimeUnit(TimeUnit.SECONDS);
        builder.setRecoveryAttemptLimit(30);
        builder.setRecoverySleepPeriod(40);

        ReceiveConfig conf = new ReceiveConfig(builder);

        assertThat(conf.getClientId(), is(CLIENT_ID));
        assertThat(conf.getIdent(), is(IDENT_FROM_TOPIC));
        assertThat(conf.getLogType(), is(LOGTYPE_FROM_TOPIC));
        assertThat(conf.getTopic(), is(LBUtils.toOldFormatTopic(TOPIC)));
        assertThat(conf.getUserLogType(), is(USER_LOG_TYPE));
        assertThat(conf.getReceiver(), is(RECEIVER));
        assertThat(conf.getLogbrokerReadOnlyAfter(),
                is(ZonedDateTime.of(2020, 2, 28, 0, 0, 0, 0, ZoneId.systemDefault())));
        assertThat(conf.isEnabled(), is(true));
        assertThat(conf.isReadOnlyLocal(), is(true));

        assertThat(conf.getReadTimeout(), is(10));
        assertThat(conf.getReceiveExecutionTimeLimit(), is(20));
        assertThat(conf.getRecoveryAttemptLimit(), is(30));
        assertThat(conf.getRecoverySleepPeriod(), is(40));

    }

    @Test
    public void testNoOptionalParamsSet() {
        ReceiveConfig.Builder builder = new ReceiveConfig.Builder();
        //required
        builder.setClientId(CLIENT_ID);
        builder.setReceiver(RECEIVER);

        //semioptional
        builder.setIdent(IDENT);
        builder.setLogType(LOGTYPE);

        ReceiveConfig conf = new ReceiveConfig(builder);

        assertThat(conf.getClientId(), is(CLIENT_ID));
        assertThat(conf.getReceiver(), is(RECEIVER));
        assertThat(conf.getIdent(), is(IDENT));
        assertThat(conf.getLogType(), is(LOGTYPE));
        //calculated
        assertThat(conf.getTopic(), is(LBUtils.toOldFormatTopic(IDENT, LOGTYPE)));
        assertThat(conf.getUserLogType(), is("chupakabra"));
        //defaults
        assertThat(conf.getLogbrokerReadOnlyAfter(),
                is(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())));
        assertThat(conf.isEnabled(), is(true));
        assertThat(conf.isReadOnlyLocal(), is(false));
        assertThat(conf.getReadTimeout(), is(5));
        assertThat(conf.getReceiveExecutionTimeLimit(), is(0));
        assertThat(conf.getRecoveryAttemptLimit(), is(0));
        assertThat(conf.getRecoverySleepPeriod(), is(60));
    }

    @Test
    public void testOptionalParamsNoIdent() {
        ReceiveConfig.Builder builder = new ReceiveConfig.Builder();
        //required
        builder.setClientId(CLIENT_ID);
        builder.setReceiver(RECEIVER);
        //semioptional
        builder.setTopic(TOPIC);

        ReceiveConfig conf = new ReceiveConfig(builder);
        assertThat(conf.getClientId(), is(CLIENT_ID));
        assertThat(conf.getReceiver(), is(RECEIVER));
        assertThat(conf.getIdent(), is(IDENT_FROM_TOPIC));
        assertThat(conf.getLogType(), is(LOGTYPE_FROM_TOPIC));
        //calculated
        assertThat(conf.getTopic(), is(LBUtils.toOldFormatTopic(TOPIC)));
        assertThat(conf.getUserLogType(), is("cuco"));
    }

}
