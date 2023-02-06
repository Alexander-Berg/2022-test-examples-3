import React from 'react';
import { shallow } from 'enzyme';

import PerfectionModal from './PerfectionModal';

describe('PerfectionModal', () => {
    it('Should render with parameters', () => {
        const wrapper = shallow(
            <PerfectionModal
                serviceName="Service Name"
                serviceId={1}
                onCloseClick={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
