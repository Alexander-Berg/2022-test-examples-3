import * as React from 'react';
import { shallow } from 'enzyme';

import { SocialBarButton as SocialBarButtonComments } from '../Button/_layout/SocialBar-Button_layout_comments';

describe('Компонент SocialBar-Button', () => {
    // полноценный unit-тест написать не вышло, т.к. любое изменение состояния
    // надо заворачивать в act(...) @see https://reactjs.org/docs/test-utils.html#act
    // но при этом он не работает с цепочками промисов в нашей версии react-dom
    // @see https://github.com/facebook/react/issues/14769
    // @see https://github.com/airbnb/enzyme/issues/2153
    // TODO: написать тесты должно получиться после обновления react-dom до 16.8.6 и выше
    test('_layout_comments должен совпадать со снепшотом', () => {
        const wrapper = shallow(
            <SocialBarButtonComments pageHash="hash" badgeCount={10} />
        );

        const deepWrapper = wrapper.dive().dive();

        expect(deepWrapper).toMatchSnapshot();
    });
});
