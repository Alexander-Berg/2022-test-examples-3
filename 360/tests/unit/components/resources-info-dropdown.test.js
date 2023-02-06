import '../noscript';
import React from 'react';
import { shallow } from 'enzyme';
import { UfoResourceInfoDropdown } from '../../../components/redux/components/resource-info-dropdown';

jest.mock('helpers/metrika');

const getProps = () => ({
    onVisible: jest.fn(),
    savedResource: {
        name: 'file',
        meta: {
            size: 12345
        },
    },
    metrikaContext: 'topbar',
    page: {},
});

describe('ResourceInfoDropdown', () => {
    it('should call props.onVisible when opened', () => {
        const props = getProps();
        const wrapper = shallow(<UfoResourceInfoDropdown {...props} />);
        wrapper.instance()._onOpen();

        expect(props.onVisible).toHaveBeenCalled();
    });
});
