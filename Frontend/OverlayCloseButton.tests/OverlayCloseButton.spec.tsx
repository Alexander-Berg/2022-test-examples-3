import * as React from 'react';
import { shallow } from 'enzyme';

import { OverlayCloseButton } from '../OverlayCloseButton';

describe('OverlayCloseButton', () => {
    beforeAll(() => {
        window.Ya = window.Ya || {};
        window.Ya.isInFrame = () => true;
    });

    afterAll(() => {
        // @ts-ignore;
        window.Ya = undefined;
    });

    test('Компонент рендерится без ошибок', () => {
        const wrapper = shallow(<OverlayCloseButton />);

        expect(wrapper.exists()).toBe(true);
    });

    test('Отправляет postmessage при клике', () => {
        const spy = jest.spyOn(window, 'postMessage');
        const wrapper = shallow(<OverlayCloseButton />);

        wrapper.simulate('click');

        expect(spy).toBeCalled();
    });
});
