package ru.yandex.market.deepmind.tracker_approver.pojo;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MyMeta {
    private final String data;

    @JsonCreator
    public MyMeta(@JsonProperty("data") String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MyMeta myMeta = (MyMeta) o;
        return Objects.equals(data, myMeta.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "MyTicketMeta{" +
            "data='" + data + '\'' +
            '}';
    }
}
