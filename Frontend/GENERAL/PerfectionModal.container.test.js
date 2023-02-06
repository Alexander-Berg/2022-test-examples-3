import React from 'react';
import { shallow } from 'enzyme';

import { PerfectionModalContainer } from './PerfectionModal.container';

describe('PerfectionModalContainer', () => {
    it('should updateService on init', () => {
        const updateService = jest.fn();

        const wrapper = shallow(
            <PerfectionModalContainer
                updateService={updateService}
                serviceId={1}
            />
        );

        expect(updateService).toHaveBeenCalled();

        wrapper.unmount();
    });
});
