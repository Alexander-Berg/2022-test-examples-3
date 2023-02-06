import React from 'react';
import { shallow } from 'enzyme';

import { PerfectionServiceInfo } from './PerfectionServiceInfo';

describe('PerfectionServiceInfo', () => {
    it('default', () => {
        const wrapper = shallow(
            <PerfectionServiceInfo serviceId={42} />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('when service is deleted', () => {
        const wrapper = shallow(
            <PerfectionServiceInfo serviceId={42} serviceState="deleted" />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
