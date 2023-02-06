import React from 'react';
import { shallow } from 'enzyme';

import PerfectionIssueLevelIcon from './PerfectionIssueLevelIcon';

describe('PerfectionIssueLevelIcon', () => {
    it('Should render with parameters', () => {
        const wrapper = shallow(
            <PerfectionIssueLevelIcon
                type="small"
                level="warning"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
