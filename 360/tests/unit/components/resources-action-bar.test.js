import '../noscript';
import React from 'react';
import { shallow } from 'enzyme';
import { UfoResourcesActionBar } from '../../../components/redux/components/resources-action-bar';

jest.mock('helpers/metrika');

const resources = [{
    name: 'folder',
    type: 'dir',
    meta: {
        lastSizeUpdatedTS: 1000,
        size: 12345
    },
}];

const getProps = () => ({
    actions: {},
    availableActions: [],
    onAction: jest.fn(),
    resources,
    savedResources: resources,
    getDirSize: jest.fn(),
});

describe('ResourcesActionBar', () => {
    beforeEach(() => {
        jest.resetAllMocks();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should call getDirSize on _updateDirStats', () => {
        const props = getProps();
        const wrapper = shallow(<UfoResourcesActionBar {...props} />);

        Date.now = jest.fn(() => 7000);

        wrapper.instance()._updateOutdatedDirStats();

        expect(props.getDirSize).toHaveBeenCalled();
    });

    it('should not call getDirSize on _updateDirStats', () => {
        const props = getProps();
        const wrapper = shallow(<UfoResourcesActionBar {...props} />);

        Date.now = jest.fn(() => 5999);

        wrapper.instance()._updateOutdatedDirStats();

        expect(props.getDirSize).not.toHaveBeenCalled();
    });
});
