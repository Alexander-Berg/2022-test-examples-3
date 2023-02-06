import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruReasons } from '../BeruReasons';
import { BeruReasonsItem } from '../BeruReasonsItem/BeruReasonsItem';
import { reasonsToProps } from './datastub';

describe('BeruReasons', () => {
    it('по умолчанию отрисовывается без ошибок', () => {
        const wrapper = shallow(<BeruReasons reasons={reasonsToProps} />);

        expect(wrapper.hasClass('beru-reasons')).toEqual(true);
    });

    it('отображается иконка и текст', () => {
        const { title, icon, info } = reasonsToProps[0];
        const wrapper = shallow(<BeruReasonsItem
            title={title}
            icon={icon}
            info={info}
        />);

        expect(wrapper.find('.beru-reasons__icon').html()).toEqual('<div class="beru-reasons__icon beru-reasons__icon beru-reasons__icon_customersChoice"></div>');
        expect(wrapper.find('.beru-reasons__title').text()).toEqual('<BeruText />');
    });
});
