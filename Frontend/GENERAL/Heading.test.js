import React from 'react';
import { render } from 'enzyme';

import { H1, H2, H3, H4, H5, H6 } from './Heading';

describe('Should render Heading', () => {
    it('level 1', () => {
        const wrapper = render(<H1>Heading 1</H1>);
        expect(wrapper).toMatchSnapshot();
    });

    it('level 2', () => {
        const wrapper = render(<H2>Heading 2</H2>);
        expect(wrapper).toMatchSnapshot();
    });

    it('level 3', () => {
        const wrapper = render(<H3>Heading 3</H3>);
        expect(wrapper).toMatchSnapshot();
    });

    it('level 4', () => {
        const wrapper = render(<H4>Heading 4</H4>);
        expect(wrapper).toMatchSnapshot();
    });

    it('level 5', () => {
        const wrapper = render(<H5>Heading 5</H5>);
        expect(wrapper).toMatchSnapshot();
    });

    it('level 6', () => {
        const wrapper = render(<H6>Heading 6</H6>);
        expect(wrapper).toMatchSnapshot();
    });
});
