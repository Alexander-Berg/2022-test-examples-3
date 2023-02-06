package ru.yandex.market.tpl.partner.carrier;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@TplPartnerCarrierWebIntTest
public abstract class BaseTplPartnerCarrierWebIntTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    public static final long UID = 1L;
    public static final long ANOTHER_UID = 2L;
    public static final long ANOTHER_UID_2 = 3L;

    @Autowired
    protected ConfigurationServiceAdapter configurationServiceAdapter;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSuper() {
    }

    @SneakyThrows
    protected String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }

    protected <T> T executeInTransaction(TransactionCallback<T> action) {
        return transactionTemplate.execute(action);
    }

    protected void runWithTransaction(Runnable action) {
        transactionTemplate.execute(tc -> {
            action.run();
            return null;
        });
    }

}
