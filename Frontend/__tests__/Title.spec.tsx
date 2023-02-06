import * as React from 'react';
import { shallow } from 'enzyme';
import { Title } from '../Title';

describe('Title', () => {
    it('Должен проставлять классы для БЭМа и для React', () => {
        const wrapper = shallow(
            <Title level={2} size="xs" />
        );
        const classes = ['title', 'turbo-title', 'title_size_xs', 'turbo-title_size_xs'];
        classes.forEach(cssClass => {
            expect(wrapper.hasClass(cssClass)).toBeTruthy();
        });
    });
});
