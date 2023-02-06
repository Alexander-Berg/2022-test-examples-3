import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruDiscountBadge, IProps } from '../BeruDiscountBadge';
import * as stubData from '../datastub';

describe('BeruDiscountBadge', () => {
    it('должен корретно отрисовываться без ошибок', () => {
        const data = stubData.dataDefault as IProps;
        const wrapper = shallow(<BeruDiscountBadge {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    it('должен корректно выводить текст', () => {
        const data = stubData.dataBigBlack as IProps;
        const wrapper = shallow(<BeruDiscountBadge {...data} />);
        expect(wrapper.render().find('.beru-discount-badge__discount').text()).toEqual(String(data.percent));
    });

    it('должен корректно выводить размер', () => {
        const data = stubData.dataBigBlack as IProps;
        const wrapper = shallow(<BeruDiscountBadge {...data} />);
        expect(wrapper.hasClass('beru-discount-badge_size_l')).toEqual(true);
    });

    it('должен корректно выводить тему', () => {
        const data = stubData.dataBigBlack as IProps;
        const wrapper = shallow(<BeruDiscountBadge {...data} />);
        expect(wrapper.hasClass('beru-discount-badge_theme_black')).toEqual(true);
    });

    it('должен корректно выводить переданный css класс', () => {
        const data = stubData.dataBigBlack as IProps;
        const wrapper = shallow(<BeruDiscountBadge {...data} className="test-class" />);
        expect(wrapper.hasClass('test-class')).toBeTruthy();
    });
});
