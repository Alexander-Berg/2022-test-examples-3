import * as React from 'react';
import { shallow } from 'enzyme';

import { DateString } from '@yandex-turbo/components/Date/Date';

import { RecommendationsCardMeta } from '../RecommendationsCard__Meta';
import { recommendationsCardCn } from '../../RecommendationsCard.cn';

describe('Компонент RecommendationsCardMeta', () => {
    test('Рендерится без ошибок', () => {
        const wrapper = shallow(<RecommendationsCardMeta />);

        expect(wrapper.exists()).toBe(true);
    });

    test('Рендерит дату', () => {
        const timestamp = new Date().getTime();
        const wrapper = shallow(<RecommendationsCardMeta date={{ timestamp }} />);
        const date = wrapper.find(DateString);
        expect(date.exists()).toBe(true);
        expect(date.hasClass(recommendationsCardCn('date-string')));
    });
});
