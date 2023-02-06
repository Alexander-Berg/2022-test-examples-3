package ru.yandex.market.mbo.core.join.yt;

public class TestReducer extends JoinReducer<String> {

    public TestReducer() {
        super(n -> n.getString("id"));
    }
}
