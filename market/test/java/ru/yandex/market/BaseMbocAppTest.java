package ru.yandex.market;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentConfig;

@ContextConfiguration(
    classes ={ OfferProcessingAssignmentConfig.class }
)
public abstract class BaseMbocAppTest extends BaseDbTestClass {
}
