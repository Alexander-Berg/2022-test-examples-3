import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import AttachmentDeleteConfirmationDialog from '../../../../../../src/components/dialogs/attachment-delete-confirmation-dialog';
import ConfirmDialog from '@ps-int/ufo-rocks/lib/components/dialogs/confirm';
import getStore from '../../../../../../src/store';
import { DIALOG_STATES } from '../../../../../../src/consts';

jest.mock('../../../../../../src/store/actions', () => ({
    closeDialog: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../../src/store/actions/attachments', () => ({
    deleteAttachment: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../../src/helpers/metrika', () => ({
    countAttachment: jest.fn()
}));

import { closeDialog } from '../../../../../../src/store/actions';
import { deleteAttachment } from '../../../../../../src/store/actions/attachments';
import { countAttachment } from '../../../../../../src/helpers/metrika';

const NOTE_ID = 'noteId';
const RESOURCE_ID = 'resourceId';
const SOURCE = 'source';
const getComponent = () => (
    <Provider
        store={getStore({
            notes: { current: NOTE_ID },
            dialogs: {
                attachmentDeleteConfirmation: {
                    state: DIALOG_STATES.OPENED,
                    data: { resourceId: RESOURCE_ID, source: SOURCE }
                }
            }
        })}
    >
        <AttachmentDeleteConfirmationDialog />
    </Provider>
);
const getButtonByCls = (wrapper, cls) => wrapper
    .find('lego-components_button')
    .filterWhere((button) => new RegExp(cls).test(button.props().className));

describe('src/components/dialogs/attachment-delete-confirmation-dialog', () => {
    it('should call metrika on dialog cancel/close', () => {
        const wrapper = mount(getComponent());

        expect(wrapper.find(ConfirmDialog).exists()).toBe(true);
        getButtonByCls(wrapper, 'confirmation-dialog__button_cancel').simulate('click');
        closeDialog('attachmentDeleteConfirmation');
        expect(popFnCalls(closeDialog)[0]).toEqual(['attachmentDeleteConfirmation']);
        expect(popFnCalls(countAttachment)[0]).toEqual(['delete', 'reject']);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should call metrika and appropriate action on dialog submit', () => {
        const wrapper = mount(getComponent());

        expect(wrapper.find(ConfirmDialog).exists()).toBe(true);
        getButtonByCls(wrapper, 'confirmation-dialog__button_submit').simulate('click');
        expect(popFnCalls(deleteAttachment)[0]).toEqual([{ noteId: NOTE_ID, resourceId: RESOURCE_ID }]);
        expect(popFnCalls(closeDialog)[0]).toEqual(['attachmentDeleteConfirmation']);
        expect(popFnCalls(countAttachment)[0]).toEqual(['delete', SOURCE]);
    });
});
