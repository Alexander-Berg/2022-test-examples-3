package ru.yandex.market.tsum.context.impl;

import org.springframework.data.mongodb.core.MongoTemplate;

import ru.yandex.market.tsum.context.ExperimentJobContext;
import ru.yandex.market.tsum.context.InternalJobContext;
import ru.yandex.market.tsum.pipelines.sre.helpers.ApproverHelper;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 28/11/2018
 */
public class TestInternalJobContext implements InternalJobContext {
    @Override
    public ApproverHelper approver() {
        return null;
    }

    @Override
    public MongoTemplate mongoTemplate() {
        return null;
    }

    @Override
    public ExperimentJobContext experiments() {
        return new TestExperimentJobContext();
    }
}
