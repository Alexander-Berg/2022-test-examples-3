import '../../noscript';
import React from 'react';
import { mount } from 'enzyme';
// import ReactTestUtils from 'react-dom/test-utils';
import serializer from 'enzyme-to-json';

jest.mock('../../../../components/extract-preloaded-data');

import { ResourceRenameDialog } from '../../../../components/redux/components/dialogs/resource-rename-dialog';
import ConfirmDialog from '@ps-int/ufo-rocks/lib/components/dialogs/confirm';
import RenameDialog from '@ps-int/ufo-rocks/lib/components/dialogs/rename';

jest.mock('config', () => ({}));

describe('ResourceRenameDialog:', () => {
    let wrapper;
    // let confirmationDialog;
    let props;
    const spies = {};
    const methods = [
        'componentDidUpdate',
        '_cancelFileExtensionChange',
        '_onRenameDialogSubmit',
        '_onRenameDialogClose',
        '_onRenameTextChange',
        '_toggleConfirmationDialogVisible',
        '_rename',
        '_cancelFileExtensionChange'
    ];

    afterEach(() => {
        // clean up DOM after each test
        // required because dialog modal appends extra div
        // on each mount
        const node = global.document.body;
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
    });

    beforeEach(() => {
        props = {
            visible: false,
            isIosSafari: false,
            onClose: jest.fn(),
            onSubmit: jest.fn(),
            resource: {
                name: 'file.pdf',
                ext: 'pdf'
            }
        };

        wrapper = mount(<ResourceRenameDialog {...props} />);

        // find the confirmation dialog
        // confirmationDialog = wrapper.find(ConfirmDialog).at(1);

        // create spies for instance methods
        methods.forEach((method) => {
            spies[method] = jest.spyOn(wrapper.instance(), method);
        });
    });

    afterAll(() => {
        jest.clearAllMocks();
    });

    it('should render', () => {
        expect(wrapper.find(RenameDialog).length).toBe(1);
        expect(wrapper.find(ConfirmDialog).length).toBe(2);
        expect(serializer(wrapper)).toMatchSnapshot();
    });

    it('should not update if dialog is not visible', () => {
        wrapper.setProps({
            resource: {
                name: 'another.pdf',
                ext: 'pdf'
            }
        });

        expect(spies.componentDidUpdate).not.toHaveBeenCalled();

        wrapper.setProps({
            resource: {
                name: 'file.pdf',
                ext: 'pdf'
            },
            visible: true
        });

        expect(spies.componentDidUpdate).toHaveBeenCalled();
    });
});
