import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { BeruRating } from '../BeruRating';

describe('BeruRating', () => {
    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruRating />);

        expect(wrapper.length).toEqual(1);
    });

    it('должен правильно отрисоваться по умолчанию', () => {
        const wrapper = shallow(<BeruRating />);

        expect(wrapper.find('.beru-rating__front').html()).toContain('style="width:0%"');
    });

    it('компонента BeruText должна корректно инициализироваться по умолчанию', () => {
        const text = '58 отзывов';
        const wrapper = shallow(<BeruRating text={text} />);

        expect(wrapper.find(BeruText).props()).toEqual({
            size: '300',
            theme: 'muted',
            weight: 'regular',
            children: text,
        });
    });

    it('значение размера компонента должно корректно задавать размер текста', () => {
        const wrapper = shallow(<BeruRating text="test" />);
        const map = { xs: '100', s: '200', m: '300', l: '300' };

        for (let size in map) {
            wrapper.setProps({ size });
            expect(wrapper.find(BeruText).prop('size')).toEqual(map[size]);
        }
    });

    it('должен правильно устанавливать значение рейтинга', () => {
        const wrapper = shallow(<BeruRating value={4} />);

        expect(wrapper.find('.beru-rating__front').html()).toContain('style="width:80%"');
    });
});
