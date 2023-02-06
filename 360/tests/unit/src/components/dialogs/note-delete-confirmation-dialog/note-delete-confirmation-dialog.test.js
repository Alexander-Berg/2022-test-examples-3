import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import NoteDeleteConfirmationDialog from '../../../../../../src/components/dialogs/note-delete-confirmation-dialog';
import ConfirmDialog from '@ps-int/ufo-rocks/lib/components/dialogs/confirm';
import getStore from '../../../../../../src/store';
import { DIALOG_STATES } from '../../../../../../src/consts';

jest.mock('../../../../../../src/store/actions', () => ({
    closeDialog: jest.fn(() => ({ type: '' })),
    deleteCurrent: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn()
}));

import { closeDialog, deleteCurrent } from '../../../../../../src/store/actions';
import { countNote } from '../../../../../../src/helpers/metrika';

const getComponent = () => (
    <Provider
        store={getStore({
            dialogs: {
                noteDeleteConfirmation: { state: DIALOG_STATES.OPENED }
            }
        })}
    >
        <NoteDeleteConfirmationDialog />
    </Provider>
);
const getButtonByCls = (wrapper, cls) => wrapper
    .find('lego-components_button')
    .filterWhere((button) => new RegExp(cls).test(button.props().className));

describe('src/components/dialogs/note-delete-confirmation-dialog', () => {
    it('should call metrika on dialog cancel/close', () => {
        const wrapper = mount(getComponent());

        expect(wrapper.find(ConfirmDialog).exists()).toBe(true);
        getButtonByCls(wrapper, 'confirmation-dialog__button_cancel').simulate('click');
        expect(popFnCalls(closeDialog)[0]).toEqual(['noteDeleteConfirmation']);
        expect(popFnCalls(countNote)[0]).toEqual(['delete', 'reject']);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should call metrika and appropriate action on dialog submit', () => {
        const wrapper = mount(getComponent());

        expect(wrapper.find(ConfirmDialog).exists()).toBe(true);
        getButtonByCls(wrapper, 'confirmation-dialog__button_submit').simulate('click');
        expect(popFnCalls(deleteCurrent)[0]).toEqual([]);
        expect(popFnCalls(closeDialog)[0]).toEqual(['noteDeleteConfirmation']);
        expect(popFnCalls(countNote)[0]).toEqual(['delete', 'submit']);
    });
});
