import * as React from 'react';
import { shallow } from 'enzyme';

import { Skeleton } from '../Skeleton';

describe('Компонент Skeleton', () => {
    test('Рендерит указаное количество блоков', () => {
        const expected = 10;
        const wrapper = shallow(<Skeleton repeatCount={expected} />);

        expect(wrapper.find('.turbo-skeleton__item')).toHaveLength(expected);
    });

    test('Передает указанную ширину и высоту в item', () => {
        const wrapper = shallow(<Skeleton height="200px" width="100%" />);
        const item = wrapper.find('.turbo-skeleton__item');

        expect(item.prop('style')).toEqual({ height: '200px', width: '100%' });
    });

    test('Устанавливает рандомную ширину, если ширина не передана явно', () => {
        const returnedRandomValue = 0.123456789;
        const spy = jest.spyOn(global.Math, 'random').mockReturnValue(returnedRandomValue);
        const wrapper = shallow(<Skeleton height="200px" />);
        const item = wrapper.find('.turbo-skeleton__item');

        expect(item.prop('style')).toEqual({ height: '200px', width: `${100 - Math.floor(returnedRandomValue * 40)}%` });

        spy.mockRestore();
    });
});
