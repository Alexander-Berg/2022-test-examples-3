package ru.yandex.market.crm.triggers.services.bpm;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.crm.core.domain.trigger.MessagePurpose;
import ru.yandex.market.crm.core.domain.trigger.MessageTypeDto;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.core.triggers.ExecutionListeners;
import ru.yandex.market.crm.triggers.services.bpm.meta.MessageTypesDAO;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;
import ru.yandex.market.mcrm.db.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author apershukov
 */
public class TriggerMessageTypesInitializerTest extends AbstractServiceTest {

    @Inject
    private MessageTypesDAO messageTypesDAO;

    @Inject
    private TriggerMessageTypesInitializer initializer;

    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testInitialize() {
        MessageTypeDto staleType = MessageTypeDto.builder()
                .setId("stale-message")
                .setName("Message Title")
                .setListener("listener")
                .build();

        messageTypesDAO.addMessageTypes(Collections.singletonList(staleType), MessagePurpose.START);

        initializer.init();

        Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trigger_message_types",
                Integer.class
        );
        assertNotEquals(0, (int) totalCount);

        Integer staleCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trigger_message_types\n" +
                        "WHERE id = ?",
                Integer.class,
                staleType.getId()
        );

        assertEquals(0, (int) staleCount);

        List<MessageTypeDto> types = jdbcTemplate.query(
                "SELECT * FROM trigger_message_types\n" +
                        "WHERE id = ? AND purpose = ?",
                (rs, i) -> MessageTypeDto.builder()
                        .setId(rs.getString("id"))
                        .setName(rs.getString("name"))
                        .setListener(rs.getString("listener"))
                        .build(),
                MessageTypes.CART_ITEM_ADDED,
                MessagePurpose.START.name()
        );

        assertEquals(1, types.size());
        assertEquals("Добавление в корзину", types.get(0).getName());
        assertEquals(ExecutionListeners.CART_ITEM_CHANGE, types.get(0).getListener());
    }
}
