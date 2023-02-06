package ru.yandex.market.psku.postprocessor.service.tracker.mock;

import ru.yandex.startrek.client.model.Component;

public class ComponentMock extends Component {

    protected ComponentMock() {
        super(1L, null, 1, null, null, null, null, true, null);
    }

    @Override
    public Long getId() {
        return 1L;
    }

    @Override
    public long getVersion() {
        return 1;
    }

}
