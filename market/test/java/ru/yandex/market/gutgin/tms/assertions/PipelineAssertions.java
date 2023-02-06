package ru.yandex.market.gutgin.tms.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;

/**
 * @author danfertev
 * @since 29.07.2019
 */
public class PipelineAssertions extends AbstractObjectAssert<PipelineAssertions, Pipeline> {
    public PipelineAssertions(Pipeline pipeline) {
        super(pipeline, PipelineAssertions.class);
    }

    public PipelineAssertions hasStatus(MrgrienPipelineStatus expectedStatus) {
        super.isNotNull();
        Assertions.assertThat(actual.getStatus()).isEqualTo(expectedStatus);
        return myself;
    }
}
