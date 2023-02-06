import React from 'react';

import { shallow } from 'enzyme';
import { NoItems } from './NoItems';

describe('UI/NoItems', () => {
    it('should render NoItems', () => {
        const wrapper = shallow(<NoItems>Nothing to watch</NoItems>);
        expect(wrapper).toMatchSnapshot();
    });
});
