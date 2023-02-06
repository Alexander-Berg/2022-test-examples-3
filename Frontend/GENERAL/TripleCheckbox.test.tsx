import React from 'react';

import { shallow } from 'enzyme';
import { TripleCheckbox } from './TripleCheckbox';

describe('UI/TripleCheckbox', () => {
    it('should render TripleCheckbox with set value', () => {
        const wrapper = shallow(<TripleCheckbox value="set" onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render TripleCheckbox with different value', () => {
        const wrapper = shallow(<TripleCheckbox value="different" onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render TripleCheckbox with unset value', () => {
        const wrapper = shallow(<TripleCheckbox value="unset" onChange={() => null} />);
        expect(wrapper).toMatchSnapshot();
    });
});
