import React from 'react';

import { shallow } from 'enzyme';
import { BulkSelector } from './BulkSelector';

describe('UI/BulkSelector', () => {
    it('should render BulkSelector', () => {
        const wrapper = shallow(<BulkSelector showCounter items={[]} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render BulkSelector without counter', () => {
        const wrapper = shallow(<BulkSelector showCounter={false} items={[]} />);
        expect(wrapper).toMatchSnapshot();
    });
});
