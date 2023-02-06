package ru.yandex.market.mbo.tracker;

import ru.yandex.startrek.client.model.Component;

public class ComponentMock extends Component {

    protected ComponentMock() {
        super(1L, null, 1, null, null, null, null, true, null);
    }

    public Long getId() {
        return 1L;
    }

    @Override
    public long getVersion() {
        return 1;
    }

}
