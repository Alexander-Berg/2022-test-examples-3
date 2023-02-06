import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import getStore from '../../../../../src/store';
import Note from '../../../../../src/components/note';
import NoteStateIndicator from '../../../../../src/components/note-state-indicator';
import { Textinput as TextInput } from '@ps-int/ufo-rocks/lib/components/lego-components/Textinput';
import { DESKTOP_LAYOUT_THRESHOLD, STATES } from '../../../../../src/consts';

const delay = (ms) => {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
};

const mockedEditor = {
    getSnippet: jest.fn(),
    focus: jest.fn()
};

jest.mock('../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn()
}));

jest.mock('../../../../../src/components/editor', () => ({ setRef }) => {
    setRef(mockedEditor);
    return null;
});

jest.mock('../../../../../src/store/actions', () => ({
    fetchNoteContent: jest.fn(() => ({ type: '' })),
    updateNoteTitle: jest.fn(() => ({ type: '' }))
}));

jest.mock('../../../../../src/store/actions/attachments', () => ({
    batchRequestAttachments: jest.fn(() => ({ type: '' })),
    toggleSlider: jest.fn(() => ({ type: '' }))
}));

jest.mock('@ps-int/ufo-rocks/lib/components/groupable-buttons', () => () => null);

import { countNote } from '../../../../../src/helpers/metrika';
import { fetchNoteContent, updateNoteTitle } from '../../../../../src/store/actions';
import { batchRequestAttachments } from '../../../../../src/store/actions/attachments';

const getNote = ({ state }) => ({
    someId: {
        id: 'someId',
        title: 'some-title',
        content: { state },
        attachmentOrder: [],
        tags: {}
    }
});
const getState = ({ currentNoteState, isMobile, windowWidth }) => {
    return {
        notes: {
            current: 'someId',
            notes: getNote({ state: currentNoteState }),
            blockNoteSelection: false
        },
        ua: { isMobile, isIosSafari: false },
        environment: { windowWidth }
    };
};
const getComponent = ({
    currentNoteState = STATES.INITIAL,
    isMobile = false,
    windowWidth = DESKTOP_LAYOUT_THRESHOLD
}) => (
    <Provider store={getStore(getState({ currentNoteState, isMobile, windowWidth }))}>
        <Note
            editorRef={() => {}}
        />
    </Provider>
);

describe('components/note =>', () => {
    it('should render TextInput as title component on desktop devices', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.find(TextInput).exists()).toBe(true);
        expect(wrapper.find('.note__touch-title').exists()).toBe(false);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render TextInput as title component on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ isMobile: true, windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find(TextInput).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });

    it('should render note status indicator if note is loading on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ currentNoteState: STATES.LOADING, isMobile: true,
            windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find(NoteStateIndicator).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
        global.IS_TOUCH = false;
    });

    it('should render note status indicator on desktop devices', () => {
        const wrapper = mount(getComponent({}));

        expect(wrapper.find(NoteStateIndicator).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should render note status indicator on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(getComponent({ isMobile: true, windowWidth: DESKTOP_LAYOUT_THRESHOLD - 1 }));

        expect(wrapper.find(NoteStateIndicator).exists()).toBe(true);
        global.IS_TOUCH = false;
    });

    it('should fetch note content and its attachments on update', () => {
        mount(getComponent({}));

        expect(fetchNoteContent).toBeCalled();
        expect(batchRequestAttachments).toBeCalled();
    });

    it('should call an appropriate action on title change', async() => {
        const wrapper = mount(getComponent({}));
        wrapper.find(TextInput).simulate('change', { slice: () => {}, target: { value: '' } });

        await delay(2000);

        expect(updateNoteTitle).toBeCalled();
        expect(mockedEditor.getSnippet).toBeCalled();
    });

    it('should focus on editor when keyDown is pressed inside a title component', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find(TextInput).simulate('keydown', { keyCode: 13 });
        expect(mockedEditor.focus).toBeCalled();
    });

    it('should call metrika function on title click', () => {
        const wrapper = mount(getComponent({}));

        wrapper.find(TextInput).props().onClick();
        expect(popFnCalls(countNote)[0]).toEqual(['title', 'click']);
    });
});
