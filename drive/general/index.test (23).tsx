import { mount } from 'enzyme';
import * as React from 'react';

import { ButtonWithPopup } from './component';

describe("ButtonWithPopup", () => {
    it('should render correctly', function () {
        const button = mount(
            <ButtonWithPopup popupContent={<span>Test</span>}>
                Test
            </ButtonWithPopup>,
        );

        expect(button).toMatchSnapshot();
    });

    it('should render correctly when open', function () {
        const button = mount(
            <ButtonWithPopup popupContent={<span>Test</span>}>
                Test
            </ButtonWithPopup>,
        );

        button.find('button').simulate('click');
        expect(button).toMatchSnapshot();
    });
});
