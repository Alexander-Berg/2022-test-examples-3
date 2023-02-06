import * as React from 'react';
import { shallow } from 'enzyme';

import { RecommendationsCardTitle } from '../RecommendationsCard__Title';
import { recommendationsCardCn } from '../../RecommendationsCard.cn';

describe('Компонент RecommendationsCardTitle', () => {
    test('Рендерится без ошибок', () => {
        const wrapper = shallow(
            <RecommendationsCardTitle>
                Заголовок
            </RecommendationsCardTitle>
        );

        expect(wrapper.hasClass(recommendationsCardCn('title'))).toBe(true);
        expect(wrapper.exists()).toBe(true);
    });

    test('Рендерит заголовок среднего размера по умолчанию', () => {
        const wrapper = shallow(
            <RecommendationsCardTitle>
                Заголовок
            </RecommendationsCardTitle>
        );
        const [, expectedClassname] = recommendationsCardCn('title', { size: 'm' }).split(' ');
        expect(wrapper.hasClass(expectedClassname)).toBe(true);
    });

    test('Рендерит заголовок большого размера', () => {
        const wrapper = shallow(
            <RecommendationsCardTitle size="l">
                Заголовок
            </RecommendationsCardTitle>
        );
        const [, expectedClassname] = recommendationsCardCn('title', { size: 'l' }).split(' ');
        expect(wrapper.hasClass(expectedClassname)).toBe(true);
    });

    test('Рендерит детей', () => {
        const wrapper = shallow(
            <RecommendationsCardTitle>
                <span className="expected">Заголовок</span>
            </RecommendationsCardTitle>
        );
        const content = wrapper.find('.expected');
        expect(content.exists()).toBe(true);
    });

    test('Проставляет переданный тип в качестве модификатора', () => {
        const wrapper = shallow(
            <RecommendationsCardTitle type="extended">
                Заголовок
            </RecommendationsCardTitle>
        );

        const [, expectedClassname] = recommendationsCardCn('title', { type: 'extended' }).split(' ');
        expect(wrapper.hasClass(expectedClassname)).toBe(true);
    });

    test('Проставляет модификатор типа со значением по умолчанию', () => {
        const wrapper = shallow(
            <RecommendationsCardTitle>
                Заголовок
            </RecommendationsCardTitle>
        );

        const [, expectedClassname] = recommendationsCardCn('title', { type: 'default' }).split(' ');
        expect(wrapper.hasClass(expectedClassname)).toBe(true);
    });
});
