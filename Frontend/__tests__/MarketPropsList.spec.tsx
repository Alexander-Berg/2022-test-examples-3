import * as React from 'react';
import { shallow } from 'enzyme';

import { MarketPropsList } from '@yandex-turbo/components/MarketPropsList/MarketPropsList';

const stubProps = {
    link: '//m.market.yandex-team.ru',
    props: [
        'Диагональ экрана: 15.4"',
        'Bluetooth: есть',
        'Вес: 1.2кг',
        'Цвет: синий',
    ],
};

describe('MarketPropsList', () => {
    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<MarketPropsList {...stubProps} />);
        expect(wrapper.length).toEqual(1);
    });
});
