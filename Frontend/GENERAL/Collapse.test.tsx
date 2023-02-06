import React from 'react';

import { shallow } from 'enzyme';
import { Collapse } from './Collapse';

describe('UI/Collapse', () => {
    it('should render default opened Collapse', () => {
        const wrapper = shallow(<Collapse title="Collapse" body="Collapse body" />);
        expect(wrapper).toMatchSnapshot();
    });

    it('should render closed Collapse', () => {
        const wrapper = shallow(<Collapse title="Collapse" body="Collapse body" isOpen={false} />);
        expect(wrapper).toMatchSnapshot();
    });
});
