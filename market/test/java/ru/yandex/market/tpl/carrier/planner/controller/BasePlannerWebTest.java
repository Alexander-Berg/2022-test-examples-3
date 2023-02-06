package ru.yandex.market.tpl.carrier.planner.controller;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@PlannerWebTest
public abstract class BasePlannerWebTest {
    protected static final Long UID = 1L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected Clock clock;

    protected ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(clock);
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

    @SneakyThrows
    protected String toJson(Object object) {
        return objectMapper.writeValueAsString(object);
    }

}
