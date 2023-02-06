package ru.yandex.market.mbo.taskqueue;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestTaskHandler2 extends TestTaskHandlerBase<TestTaskHandler2.Task2> {

    public static class Task2 extends TestTaskHandlerBase.Task {
        @JsonCreator
        public Task2(@JsonProperty("slot") int slot,
                     @JsonProperty("value") String value,
                     @JsonProperty("retries") int retries) {
            super(slot, value, retries);
        }

        public Task2(int slot, String value) {
            this(slot, value, 0);
        }
    }
}
