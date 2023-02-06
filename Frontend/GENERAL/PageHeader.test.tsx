import React from 'react';

import { shallow } from 'enzyme';
import { PageHeader } from './PageHeader';

describe('UI/PageHeader', () => {
    it('should render PageHeader', () => {
        const wrapper = shallow(<PageHeader title="Title" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render PageHeader with chidlren', () => {
        const wrapper = shallow(<PageHeader title="Title"><span>Children</span></PageHeader>);
        expect(wrapper).toMatchSnapshot();
    });
});
