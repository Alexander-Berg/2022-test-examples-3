import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import Toolbar, { ACTIONS } from '../../../../../src/components/toolbar';
import getStore from '../../../../../src/store';
import UploadButton from '@ps-int/ufo-rocks/lib/components/upload-button';
import { DESKTOP_LAYOUT_THRESHOLD } from '../../../../../src/consts';

jest.mock('../../../../../src/store/actions', () => ({
    openDialog: jest.fn(() => ({ type: '' })),
    togglePin: jest.fn(() => ({ type: '' })),
    updateCurrent: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../src/store/actions/attachments', () => ({
    addAttachments: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn()
}));
jest.mock('@ps-int/ufo-rocks/lib/components/groupable-buttons', () => () => null);

import GroupableButtons from '@ps-int/ufo-rocks/lib/components/groupable-buttons';

import { openDialog, togglePin, updateCurrent } from '../../../../../src/store/actions';
import { addAttachments } from '../../../../../src/store/actions/attachments';
import { countNote } from '../../../../../src/helpers/metrika';

const getState = ({ currentNoteId, isNotePinned, isMobile, windowWidth }) => ({
    notes: {
        current: currentNoteId,
        blockNoteSelection: false,
        notes: {
            someId: {
                id: 'someId',
                tags: {
                    pin: isNotePinned
                },
                attachmentOrder: []
            }
        }
    },
    ua: { isMobile, isIosSafari: false },
    environment: { windowWidth }
});
const onToolbarAction = jest.fn();
const onToolbarClick = jest.fn();
const onReturnFocus = jest.fn();
const getComponent = ({
    currentNoteId = 'someId',
    isNotePinned = false,
    isMobile = false,
    disableEditButtons = false,
    windowWidth = DESKTOP_LAYOUT_THRESHOLD
}) => (
    <Provider store={getStore(getState({ currentNoteId, isNotePinned, isMobile, windowWidth }))}>
        <Toolbar
            commandStates={{}}
            onAction={onToolbarAction}
            onClick={onToolbarClick}
            disableEditButtons={disableEditButtons}
            returnFocus={onReturnFocus}
        />
    </Provider>
);
const getButtonsByName = (wrapper, buttonName, className = 'lego-components_button') =>
    wrapper.find(className).findWhere((item) => item.html().includes(`note-toolbar__button_${buttonName}`));
const checkButtonExist = (wrapper, buttonName, className) => getButtonsByName(wrapper, buttonName, className).length === 1;

describe('components/toolbar =>', () => {
    beforeEach(() => {
        onToolbarAction.mockReset();
        onToolbarClick.mockReset();
        onReturnFocus.mockReset();
    });

    it('should render edit (without Back button) and groupped control buttons by default', () => {
        const wrapper = mount(getComponent({}));

        expect(checkButtonExist(wrapper, 'back')).toBe(false);
        expect(checkButtonExist(wrapper, 'bold')).toBe(true);
        expect(checkButtonExist(wrapper, 'italic')).toBe(true);
        expect(checkButtonExist(wrapper, 'underline')).toBe(true);
        expect(checkButtonExist(wrapper, 'strikethrough')).toBe(true);
        expect(checkButtonExist(wrapper, 'list')).toBe(true);
        expect(wrapper.find(UploadButton).exists()).toBe(true);
        expect(wrapper.find(GroupableButtons).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render edit (with Back button) and groupped control buttons in mobile layout', () => {
        const wrapper = mount(getComponent({ windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(checkButtonExist(wrapper, 'back')).toBe(true);
        expect(checkButtonExist(wrapper, 'bold')).toBe(true);
        expect(checkButtonExist(wrapper, 'italic')).toBe(true);
        expect(checkButtonExist(wrapper, 'underline')).toBe(true);
        expect(checkButtonExist(wrapper, 'strikethrough')).toBe(true);
        expect(checkButtonExist(wrapper, 'list')).toBe(true);
        expect(wrapper.find(UploadButton).exists()).toBe(true);
        expect(wrapper.find(GroupableButtons).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should not render upload button if there`s no selected note', () => {
        const wrapper = mount(getComponent({ currentNoteId: null }));

        expect(checkButtonExist(wrapper, 'back')).toBe(false);
        expect(checkButtonExist(wrapper, 'bold')).toBe(true);
        expect(checkButtonExist(wrapper, 'italic')).toBe(true);
        expect(checkButtonExist(wrapper, 'underline')).toBe(true);
        expect(checkButtonExist(wrapper, 'strikethrough')).toBe(true);
        expect(checkButtonExist(wrapper, 'list')).toBe(true);
        expect(wrapper.find(UploadButton).exists()).toBe(false);
        expect(wrapper.find(GroupableButtons).exists()).toBe(false);
        expect(wrapper.find(UploadButton).exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should not render edit (except Back button) on touch devices', () => {
        const originalIsTouch = global.IS_TOUCH;

        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ isMobile: true, windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(checkButtonExist(wrapper, 'back')).toBe(true);
        expect(checkButtonExist(wrapper, 'bold')).toBe(false);
        expect(checkButtonExist(wrapper, 'italic')).toBe(false);
        expect(checkButtonExist(wrapper, 'underline')).toBe(false);
        expect(checkButtonExist(wrapper, 'strikethrough')).toBe(false);
        expect(checkButtonExist(wrapper, 'list')).toBe(false);
        expect(wrapper.find(UploadButton).exists()).toBe(false);
        expect(wrapper.find(GroupableButtons).exists()).toBe(false);
        expect(checkButtonExist(wrapper, 'pin')).toBe(true);
        expect(checkButtonExist(wrapper, 'delete')).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = originalIsTouch;
    });

    it('should deselect current note if Back button has been clicked', () => {
        const wrapper = mount(getComponent({ windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        getButtonsByName(wrapper, 'back').first().simulate('click');
        expect(popFnCalls(updateCurrent)[0]).toEqual([]);
    });

    it('should deselect current note if Back button has been clicked on touch device', () => {
        const originalIsTouch = global.IS_TOUCH;

        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        getButtonsByName(wrapper, 'back').first().simulate('click');
        expect(updateCurrent).toBeCalledWith();

        global.IS_TOUCH = originalIsTouch;
    });

    it('should open confirmation dialog and call metrika if Delete button has been clicked', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find(GroupableButtons).props().onButtonClick(ACTIONS.DELETE_NOTE);
        expect(popFnCalls(openDialog)[0]).toEqual(['noteDeleteConfirmation']);
        expect(popFnCalls(countNote)[0]).toEqual(['delete', 'click icon']);
    });

    it('should add attachments and call metrika if file upload input has been changed', () => {
        const wrapper = mount(getComponent({}));
        const files = ['123'];

        wrapper.find('.upload-button__attach').simulate('change', { target: { files } });
        expect(addAttachments).toBeCalledTimes(1);
        expect(popFnCalls(countNote)[0]).toEqual(['attach', files.length]);
    });

    it('should make note pinned if Pin button has been clicked and note has not been pinned', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find(GroupableButtons).props().onButtonClick(ACTIONS.PIN);
        expect(popFnCalls(togglePin)[0]).toEqual([]);
        expect(popFnCalls(countNote)[0]).toEqual(['pin']);
        expect(onReturnFocus).toBeCalledTimes(1);
    });

    it('should make note unpinned if Pin button has been clicked and note has been pinned', () => {
        const wrapper = mount(getComponent({ isNotePinned: true }));

        wrapper.find(GroupableButtons).props().onButtonClick(ACTIONS.PIN);
        expect(popFnCalls(togglePin)[0]).toEqual([]);
        expect(popFnCalls(countNote)[0]).toEqual(['unpin']);
        expect(onReturnFocus).toBeCalledTimes(1);
    });

    it('should process click on each of edit buttons', () => {
        const wrapper = mount(getComponent({}));

        ['bold', 'italic', 'underline', 'strikethrough', 'bulletedList'].forEach((buttonName) => {
            getButtonsByName(wrapper, buttonName === 'bulletedList' ? 'list' : buttonName).first().simulate('click');

            expect(popFnCalls(onToolbarAction)[0]).toEqual([buttonName]);
            expect(popFnCalls(countNote)[0]).toEqual([buttonName]);
        });
    });

    it('should process click on toolbar', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find('.note-toolbar').simulate('click');
        expect(onToolbarClick).toBeCalledTimes(1);
    });
});
