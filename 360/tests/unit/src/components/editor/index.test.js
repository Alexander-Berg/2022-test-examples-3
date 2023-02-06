import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import Editor from '../../../../../src/components/editor';
import getStore from '../../../../../src/store';
import { STATES, ERRORS } from '../../../../../src/consts';

const mockedEditor = {
    getSnippet: jest.fn(() => ''),
    getTextLength: jest.fn(() => 1)
};
jest.mock('@ps-int/ufo-rocks/lib/components/with-debounced-change',
    () => (WrappedComponent) => WrappedComponent);
jest.mock('../../../../../src/components/editor/editor', () => ({ setRef }) => {
    setRef(mockedEditor);
    return null;
});
jest.mock('../../../../../src/store/actions/content', () => ({
    updateNoteContent: jest.fn(() => ({ type: '' }))
}));
jest.mock('../../../../../src/store/actions/common', () => ({
    handleError: jest.fn(() => ({ type: '' }))
}));

import { updateNoteContent } from '../../../../../src/store/actions/content';
import { handleError } from '../../../../../src/store/actions/common';
import MockedBaseEditor from '../../../../../src/components/editor/editor';

const onSetRef = jest.fn();
const onEditorFocus = jest.fn();
const onCommandsChanged = jest.fn();
const NOTE_ID = 'note1';
const getNote = ({ noteState }) => ({
    id: NOTE_ID,
    title: 'note title',
    content: {
        state: noteState,
        data: { children: [] }
    }
});
const getState = ({ noteState }) => ({
    notes: {
        notes: { [NOTE_ID]: getNote({ noteState }) }
    },
    environment: { broMode: false }
});
const getComponent = ({
    isNoteSelected = true,
    noteState = STATES.LOADED
}) => (
    <Provider store={getStore(getState({ noteState }))}>
        <Editor
            note={isNoteSelected ? getNote({ noteState }) : null}
            setRef={onSetRef}
            onCommandsChanged={onCommandsChanged}
            onFocus={onEditorFocus}
        />
    </Provider>
);

describe('src/components/editor =>', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should call an appropriate action on base editor content change', () => {
        const data = { name: '$root', children: [] };
        const wrapper = mount(getComponent({}));
        const baseEditorProps = wrapper.find(MockedBaseEditor).props();

        expect(baseEditorProps.editorDisabled).toBe(false);
        baseEditorProps.onChange(data, NOTE_ID);
        expect(popFnCalls(updateNoteContent)[0]).toEqual([
            NOTE_ID,
            {
                content: data,
                snippet: '',
                length: 1
            }
        ]);
    });

    it('should disable base editor if there is no selected note', () => {
        const wrapper = mount(getComponent({ isNoteSelected: false }));

        expect(wrapper.find(MockedBaseEditor).props().editorDisabled).toBe(true);
    });

    it('should disable base editor if selected note is not loaded', () => {
        const wrapper = mount(getComponent({ noteState: STATES.LOADING }));

        expect(wrapper.find(MockedBaseEditor).props().editorDisabled).toBe(true);
    });

    it('should call an appropriate action when an editor`s text limit is exceeded', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find(MockedBaseEditor).props().notifyTooBigClipboard();
        expect(popFnCalls(handleError)[0]).toEqual([ERRORS.TOO_BIG_CLIPBOARD_TO_PASTE]);
    });
});
