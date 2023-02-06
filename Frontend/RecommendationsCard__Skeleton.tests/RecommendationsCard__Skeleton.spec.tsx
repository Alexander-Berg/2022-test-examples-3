import * as React from 'react';
import { shallow } from 'enzyme';

import { RecommendationsCardSkeleton } from '../RecommendationsCard__Skeleton';

describe('Компонент RecommendationsCardSkeleton', () => {
    test('Структура маленького скелетона соответствует снепшоту', () => {
        const wrapper = shallow(<RecommendationsCardSkeleton type="snippet" />);

        expect(wrapper).toMatchSnapshot();
    });
    test('Структура большого скелетона соответствует снепшоту', () => {
        const wrapper = shallow(<RecommendationsCardSkeleton type="full" />);

        expect(wrapper).toMatchSnapshot();
    });
});
