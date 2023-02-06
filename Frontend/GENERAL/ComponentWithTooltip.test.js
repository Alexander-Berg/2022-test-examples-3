import React from 'react';
import { render } from 'enzyme';

import { ComponentWithTooltip } from './ComponentWithTooltip';

describe('Should render ComponentWithTooltip', () => {
    it('Should render ComponentWithTooltip showed by hovered', () => {
        const wrapper = render(
            <ComponentWithTooltip text="Привет">
                <p>Наведи</p>
            </ComponentWithTooltip>
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should render ComponentWithTooltip showed by click', () => {
        const wrapper = render(
            <ComponentWithTooltip showByClick text="Привет">
                <p>Кликни</p>
            </ComponentWithTooltip>
        );

        expect(wrapper).toMatchSnapshot();
    });
});
