jest.mock('../../../../../../src/components/editor/helpers', () => ({
    processStringContent: jest.fn(() => '')
}));

import React from 'react';
import { mount } from 'enzyme';
import TouchEditor from '../../../../../../src/components/editor/touch-editor';
import { processStringContent } from '../../../../../../src/components/editor/helpers';
import { MAX_NOTE_CONTENT_LENGTH } from '../../../../../../src/consts';

const mockedSetInitialData = jest.fn();
const mockedOnSetRef = jest.fn();
const mockedOnInput = jest.fn();
const mockedOnPasteLimitExceeded = jest.fn();
const mockedPasteEvent = {
    clipboardData: { getData() {} },
    preventDefault: jest.fn()
};

const getComponent = ({ editorDisabled = false } = {}) => (
    <TouchEditor
        onSetRef={mockedOnSetRef}
        setInitialData={mockedSetInitialData}
        onInput={mockedOnInput}
        onPasteLimitExceeded={mockedOnPasteLimitExceeded}
        disabled={editorDisabled}
    />
);

describe('src/components/editor/touch-editor', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should call ref callback and set initial data to editor on mount', () => {
        const wrapper = mount(getComponent());

        expect(mockedOnSetRef).toBeCalled();
        expect(mockedSetInitialData).toBeCalled();
        expect(wrapper).toMatchSnapshot();
    });

    it('should call onInput if editor`s content has been changed', () => {
        const wrapper = mount(getComponent());

        wrapper.find('.touch-editor__content').simulate('input');
        expect(mockedOnInput).toBeCalled();
    });

    it('should not paste content into an editor and call an appropriate handler if a pasted content exceeds a limit', () => {
        processStringContent.mockImplementationOnce(() => 'a'.repeat(MAX_NOTE_CONTENT_LENGTH + 1));

        const wrapper = mount(getComponent());

        wrapper.find('.touch-editor__content').simulate('paste', mockedPasteEvent);
        expect(mockedPasteEvent.preventDefault).toBeCalled();
        expect(mockedOnPasteLimitExceeded).toBeCalled();
    });

    it('should paste content into an editor if a pasted content is bellow a limit', () => {
        processStringContent.mockImplementationOnce(() => '<p></p>');

        const wrapper = mount(getComponent());

        wrapper.find('.touch-editor__content').simulate('paste', mockedPasteEvent);
        expect(mockedPasteEvent.preventDefault).not.toBeCalled();
        expect(mockedOnPasteLimitExceeded).not.toBeCalled();
    });

    it('should set contenteditable attribute to false if editor is disabled', () => {
        const wrapper = mount(getComponent({ editorDisabled: true }));

        expect(wrapper).toMatchSnapshot();
    });
});
