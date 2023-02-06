package ru.yandex.market.deepmind.tracker_approver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.tracker_approver.config.PgInitializer;
import ru.yandex.market.deepmind.tracker_approver.config.TestConfig;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;

@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = PgInitializer.class,
    classes = {TestConfig.class}
)
@Transactional
public abstract class BaseTrackerApproverTest {
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected TrackerApproverDataRepository dataRepository;
    @Autowired
    protected TrackerApproverTicketRepository ticketRepository;
    @Autowired
    protected TransactionTemplate transactionTemplate;
}
