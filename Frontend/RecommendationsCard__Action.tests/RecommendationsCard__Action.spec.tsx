import * as React from 'react';
import { shallow } from 'enzyme';

import { Link } from '@yandex-turbo/components/Link/Link';

import { RecommendationsCardAction } from '../RecommendationsCard__Action';
import { recommendationsCardCn } from '../../RecommendationsCard.cn';

describe('Компонент RecommendationsCardAction', () => {
    test('Рендерится без ошибок', () => {
        const wrapper = shallow(
            <RecommendationsCardAction>
                Читать далее
            </RecommendationsCardAction>
        );

        expect(wrapper.hasClass(recommendationsCardCn('action')));
        expect(wrapper.exists()).toBe(true);
    });

    test('Имеет признак интерактивности, если передан url', () => {
        const wrapper = shallow(
            <RecommendationsCardAction url="https://yandex.ru/turbo?text=123123">
                Читать далее
            </RecommendationsCardAction>
        );

        const [, expectedClass] = recommendationsCardCn('action', { interactive: true }).split(' ');
        expect(wrapper.hasClass(expectedClass)).toBe(true);
    });

    test('Вызывает обработчик клика', () => {
        const onClick = jest.fn();
        const wrapper = shallow(
            <RecommendationsCardAction
                onClick={onClick}
                url="https://yandex.ru/turbo?text=123123"
            >
                Читать далее
            </RecommendationsCardAction>
        );

        wrapper.find(Link).simulate('click');

        expect(onClick).toBeCalled();
    });
});
