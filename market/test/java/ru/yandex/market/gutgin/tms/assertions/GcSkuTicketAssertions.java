package ru.yandex.market.gutgin.tms.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

/**
 * @author dergachevfv
 * @since 5/13/20
 */
public class GcSkuTicketAssertions extends AbstractObjectAssert<GcSkuTicketAssertions, GcSkuTicket> {

    public GcSkuTicketAssertions(GcSkuTicket gcSkuTicket) {
        super(gcSkuTicket, GcSkuTicketAssertions.class);
    }

    public GcSkuTicketAssertions hasStatus(GcSkuTicketStatus expectedStatus) {
        super.isNotNull();
        Assertions.assertThat(actual.getStatus()).isEqualTo(expectedStatus);
        return myself;
    }
}
