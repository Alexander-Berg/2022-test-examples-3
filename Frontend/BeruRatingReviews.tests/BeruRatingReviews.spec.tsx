import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruRating } from '@yandex-turbo/components/BeruRating/BeruRating';
import { BeruRatingReviews } from '../BeruRatingReviews';

describe('BeruRatingReviews', () => {
    it('по умолчанию рендерится корректно', () => {
        const wrapper = shallow(<BeruRatingReviews />);

        expect(wrapper.find(BeruRating).props()).toEqual({
            value: 0,
            text: 'Нет отзывов',
            size: undefined,
        });
    });

    it('корректно рендерится c различными значениями opinions', () => {
        const wrapper = shallow(<BeruRatingReviews opinions={3} />);

        expect(wrapper.find(BeruRating).prop('text')).toEqual('3 отзыва');

        wrapper.setProps({
            opinions: 0,
        });

        expect(wrapper.find(BeruRating).prop('text')).toEqual('Нет отзывов');
    });
});
