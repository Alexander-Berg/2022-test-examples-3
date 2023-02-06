package ru.yandex.market.gutgin.tms.assertions;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.entity.goodcontent.PmodelGroup;

/**
 * @author s-ermakov
 */
public class GutginAssertions {
    private GutginAssertions() {
    }

    public static GcSkuTicketAssertions assertThat(GcSkuTicket ticket) {
        return new GcSkuTicketAssertions(ticket);
    }

    public static ProcessTaskResultAssertions assertThat(ProcessTaskResult processTaskResult) {
        return new ProcessTaskResultAssertions(processTaskResult);
    }

    public static PmodelGroupAssertions assertThat(PmodelGroup pmodelGroup) {
        return new PmodelGroupAssertions(pmodelGroup);
    }

    public static ModelAssertions assertThat(ModelStorage.Model model) {
        return new ModelAssertions(model);
    }

    public static FileInfoResponseAssertions assertThat(PartnerContent.FileInfoResponse response) {
        return new FileInfoResponseAssertions(response);
    }

    public static PipelineAssertions assertThat(Pipeline pipeline) {
        return new PipelineAssertions(pipeline);
    }

    public static ModelTransitionAssertions assertThat(ModelStorage.ModelTransition modelTransition) {
        return new ModelTransitionAssertions(modelTransition);
    }
}
