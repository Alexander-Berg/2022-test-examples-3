import '../../noscript';
import React from 'react';
import { shallow } from 'enzyme';
import serializer from 'enzyme-to-json';

jest.mock('../../../../components/extract-preloaded-data');
jest.mock('config', () => ({}));
jest.mock('../../../../components/models/states/state-select-folder', () => ({}));
jest.mock('../../../../components/views/tree/tree', () => ({}));

import { SelectFolderDialog } from '../../../../components/redux/components/dialogs/select-folder';

describe('SelectFolderDialog', () => {
    const originalNsModelGet = ns.Model.get;
    beforeAll(() => {
        ns.Model.get = jest.fn(() => ({
            getData: () => ({}),
            get: () => {},
            on: () => {}
        }));
    });
    afterAll(() => {
        ns.Model.get = originalNsModelGet;
    });

    it('should render dialog', () => {
        const wrapper = shallow(
            <SelectFolderDialog
                visible
                folderId="/disk"
                title="select-folder dialog title"
                submitButtonText="select-folder dialog submit button text"
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
                disableState={{}}
            />
        );
        expect(serializer(wrapper)).toMatchSnapshot();
    });

    it('should render dialog with warning', () => {
        const wrapper = shallow(
            <SelectFolderDialog
                visible
                folderId="/disk"
                title="select-folder dialog title"
                submitButtonText="select-folder dialog submit button text"
                unlimWarningText="select-folder dialog warning text"
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
                disableState={{}}
            />
        );
        expect(serializer(wrapper)).toMatchSnapshot();
    });

    it('should render create folder dialog', () => {
        const wrapper = shallow(
            <SelectFolderDialog
                visible
                folderId="/disk"
                title="select-folder dialog (create folder) title"
                submitButtonText="select-folder dialog (create folder) submit button text"
                type="create-folder"
                onSubmit={jest.fn()}
                onCancel={jest.fn()}
                disableState={{}}
            />
        );
        expect(serializer(wrapper)).toMatchSnapshot();
    });
});
