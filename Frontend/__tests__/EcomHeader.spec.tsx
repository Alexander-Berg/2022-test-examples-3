import * as React from 'react';
import { shallow } from 'enzyme';

import { EcomHeaderPresenter } from '../EcomHeader';
import { EThemes } from '../EcomHeader.types';

class StubReactComponent extends React.Component {
    render() {
        return <div />;
    }
}

describe('EcomHeader компонент', () => {
    beforeEach(() => {
        // @ts-ignore
        global.window.Ya = {};
    });

    afterEach(() => {
        // @ts-ignore
        delete global.window.Ya;
    });

    it('Должен рендериться без ошибок', () => {
        const wrapper = shallow(
            <EcomHeaderPresenter theme={EThemes.DEFAULT} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('Должен рендериться без ошибок с квадратным логотипом', () => {
        const wrapper = shallow(
            <EcomHeaderPresenter
                theme={EThemes.SQUARE}
                url="https://yandex.ru"
                title="Заголовок"
                subtitle="Подзаголовок"
                logo="http://avatars.mds.yandex.net/get-turbo/3183277/2a0000017260d84ef251526537074765db4e/orig"
            />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('Должен рендерить компонент в слоте title', () => {
        const slotTitle = <StubReactComponent />;
        const wrapper = shallow(
            <EcomHeaderPresenter theme={EThemes.DEFAULT} titleBlock={slotTitle} />
        );

        expect(wrapper.find(StubReactComponent)).toHaveLength(1);
    });

    it('Должен рендерить компоненты в слотах title и aside вместе', () => {
        const slotAside = <StubReactComponent />;
        const slotTitle = <StubReactComponent />;
        const wrapper = shallow(
            <EcomHeaderPresenter
                theme={EThemes.DEFAULT}
                asideBlock={slotAside}
                titleBlock={slotTitle}
            />
        );

        expect(wrapper.find(StubReactComponent)).toHaveLength(2);
    });
});
