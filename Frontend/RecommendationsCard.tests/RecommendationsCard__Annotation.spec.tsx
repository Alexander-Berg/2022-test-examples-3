import * as React from 'react';
import { shallow } from 'enzyme';

import { RecommendationsCardAnnotation } from '../RecommendationsCard__Annotation';
import { recommendationsCardCn } from '../../RecommendationsCard.cn';

describe('Компонент RecommendationsCardTitle', () => {
    test('Рендерится без ошибок', () => {
        const wrapper = shallow(
            <RecommendationsCardAnnotation>
                О возможностях бесплатной программы LightBulb,
                реализующей в любой из версий Windows чрезвычайно
                полезную для здоровья пользователя функцию смены спектра излучения
                экрана в сторону тёплых оттенков. Аналог нативной функции «Ночной свет» Windows 10.
            </RecommendationsCardAnnotation>
        );
        expect(wrapper.hasClass(recommendationsCardCn('annotation'))).toBe(true);
        expect(wrapper.exists()).toBe(true);
    });

    test('Рендерит детей', () => {
        const wrapper = shallow(
            <RecommendationsCardAnnotation>
                <span className="expected">
                    О возможностях бесплатной программы LightBulb,
                    реализующей в любой из версий Windows чрезвычайно
                    полезную для здоровья пользователя функцию смены спектра излучения
                    экрана в сторону тёплых оттенков. Аналог нативной функции «Ночной свет» Windows 10.
                </span>
            </RecommendationsCardAnnotation>
        );
        const content = wrapper.find('.expected');
        expect(content.exists()).toBe(true);
    });

    test('Проставляет переданный тип в качестве модификатора', () => {
        const wrapper = shallow(
            <RecommendationsCardAnnotation type="extended">
                О возможностях бесплатной программы LightBulb,
                реализующей в любой из версий Windows чрезвычайно
                полезную для здоровья пользователя функцию смены спектра излучения
                экрана в сторону тёплых оттенков. Аналог нативной функции «Ночной свет» Windows 10.
            </RecommendationsCardAnnotation>
        );
        expect(wrapper.hasClass(recommendationsCardCn('annotation', { type: 'extended' }))).toBe(true);
    });

    test('Проставляет модификатор типа со значением по умолчанию', () => {
        const wrapper = shallow(
            <RecommendationsCardAnnotation>
                О возможностях бесплатной программы LightBulb,
                реализующей в любой из версий Windows чрезвычайно
                полезную для здоровья пользователя функцию смены спектра излучения
                экрана в сторону тёплых оттенков. Аналог нативной функции «Ночной свет» Windows 10.
            </RecommendationsCardAnnotation>
        );
        expect(wrapper.hasClass(recommendationsCardCn('annotation', { type: 'default' }))).toBe(true);
    });
});
