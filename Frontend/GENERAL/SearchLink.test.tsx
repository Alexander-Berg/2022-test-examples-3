import React from 'react';

import { shallow } from 'enzyme';
import { SearchLink } from './SearchLink';

describe('UI/SearchLink', () => {
    it('should render default SearchLink', () => {
        const wrapper = shallow(<SearchLink type="banner" id={{ label: 'label', value: 'value', id: 'group' }} />);
        expect(wrapper).toMatchSnapshot();
    });
});
