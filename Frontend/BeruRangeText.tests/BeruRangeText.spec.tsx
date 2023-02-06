import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruRangeText } from '../BeruRangeText';

describe('BeruRangeText', () => {
    it('По умолчанию рендерится корректно', () => {
        expect(shallow(<BeruRangeText />)).toHaveLength(1);
    });

    it('отрисовывается корректно, если передано только минимальное значение', () => {
        expect(shallow(<BeruRangeText min="320" />).text()).toEqual('от 320');
    });

    it('отрисовывается корректно, если передано только максимальное значение', () => {
        expect(shallow(<BeruRangeText max="320" />).text()).toEqual('до 320');
    });

    it('отрисовывается корректно, если передано оба значениея(максимальное и минимальное)', () => {
        expect(shallow(<BeruRangeText min="100" max="320" />).text()).toEqual('100 — 320');
    });

    it('отображает переданный unit только, если передано максимальное или минимально значение либо оба', () => {
        expect(shallow(<BeruRangeText min="100" max="320" unit="₽" />).text()).toEqual('100 — 320 ₽');
        expect(shallow(<BeruRangeText unit="₽" />).text()).toEqual('');
    });

    it('className должен корректно передаваться на рутовую ноду', () => {
        expect(shallow(<BeruRangeText className="test" />).hasClass('test')).toEqual(true);
    });
});
