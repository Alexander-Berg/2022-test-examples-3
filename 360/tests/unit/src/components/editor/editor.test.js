import React from 'react';
import { mount } from 'enzyme';
import { STATES, MAX_NOTE_CONTENT_LENGTH } from '../../../../../src/consts';

jest.mock('lodash/debounce', () => (func) => func);
jest.mock('@ckeditor/ckeditor5-engine/src/model/element', () => ({}));
jest.mock('@ckeditor/ckeditor5-clipboard/src/utils/viewtoplaintext', () => () => '');
jest.mock('@ckeditor/ckeditor5-engine/src/model/text', () => ({}));
jest.mock('@ckeditor/ckeditor5-engine/src/model/range', () => ({}));
jest.mock('@ckeditor/ckeditor5-react', () => () => null);
jest.mock('@ckeditor/ckeditor5-editor-classic/src/classiceditor', () => ({}));
jest.mock('@ckeditor/ckeditor5-block-quote/src/blockquote', () => ({}));
jest.mock('@ckeditor/ckeditor5-basic-styles/src/bold', () => ({}));
jest.mock('@ckeditor/ckeditor5-basic-styles/src/strikethrough', () => ({}));
jest.mock('@ckeditor/ckeditor5-enter/src/enter', () => ({}));
jest.mock('@ckeditor/ckeditor5-heading/src/heading', () => ({}));
jest.mock('@ckeditor/ckeditor5-basic-styles/src/italic', () => ({}));
jest.mock('@ckeditor/ckeditor5-list/src/list', () => ({}));
jest.mock('@ckeditor/ckeditor5-paragraph/src/paragraph', () => ({}));
jest.mock('@ckeditor/ckeditor5-typing/src/typing', () => ({}));
jest.mock('@ckeditor/ckeditor5-basic-styles/src/underline', () => ({}));
jest.mock('@ckeditor/ckeditor5-undo/src/undo', () => ({}));
jest.mock('../../../../../src/components/editor/ckeditor5-autoformat/src/autoformat', () => ({}));
jest.mock('../../../../../src/components/editor/ckeditor5-checkbox/src/checkbox', () => ({}));
jest.mock('../../../../../src/components/editor/ckeditor5-clipboard/src/clipboard', () => ({ default: () => ({}) }));
jest.mock('../../../../../src/components/editor/ckeditor5-image/src/image', () => ({}));
jest.mock('../../../../../src/components/editor/ckeditor5-link/src/link', () => ({}));
jest.mock('../../../../../src/components/editor/ckeditor5-textalignment', () => ({}));
jest.mock('../../../../../src/components/editor/helpers', () => ({
    dataIsEmpty: jest.fn(() => false),
    getPlainText: jest.fn(() => ''),
    processTouchEditorHtmlContent: jest.fn()
}));
jest.mock('../../../../../src/components/editor/setData', () => jest.fn());
jest.mock('../../../../../src/components/editor/getCommandsState', () => jest.fn());
jest.mock('../../../../../src/components/editor/getData', () => jest.fn());
jest.mock('../../../../../src/components/editor/getSnippet', () => jest.fn((_, title) => title));
jest.mock('../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn(),
    countError: jest.fn(),
    countObj: jest.fn()
}));
jest.mock('@ps-int/ufo-rocks/lib/helpers/offline');

let listenersMap = {};
const mockedCommand = {
    on: (eventName, callback) => {
        listenersMap[eventName] = callback;
    }
};
const mockedGetEditorData = jest.fn(() => '');
const mockedEditor = {
    commands: { commands: () => [mockedCommand] },
    listenTo: (_, eventName, listener) => {
        listenersMap[eventName] = listener;
    },
    fire(eventName, params) {
        if (listenersMap[eventName]) {
            listenersMap[eventName](null, params);
        }
    },
    model: {
        document: {
            on: (eventName, listener) => {
                listenersMap[eventName] = listener;
            }
        }
    },
    editing: {
        view: {
            document: {
                on: (eventName, listener) => {
                    listenersMap[eventName] = listener;
                }
            }
        }
    },
    getData: mockedGetEditorData
};
const noteId = 'note1';
const getNote = ({ noteState, children }) => ({
    id: noteId,
    title: 'note title',
    content: {
        state: noteState,
        data: { children }
    }
});
const onInitEditor = jest.fn();
const onEditorEnabled = jest.fn();
const onEditorFocus = jest.fn();
const onChange = jest.fn();
const onCommandsChanged = jest.fn();
const notifyTooBigClipboard = jest.fn();
const setRef = jest.fn();
const getComponent = ({
    noteState = STATES.LOADED,
    children = [],
    note = getNote({ noteState, children }),
    broMode = false,
    editorDisabled = false
}) => {
    const BaseEditor = require('../../../../../src/components/editor/editor').default;
    const props = note && note.content.state === STATES.LOADED ? {
        id: note.id,
        noteData: note.content.data,
        noteTitle: note.title,
        state: noteState,
        saving: note.saving,
        conflict: note.conflict,
        onFocus: onEditorFocus,
        editorDisabled
    } : { editorDisabled: true };

    return (
        <BaseEditor
            {...props}
            broMode={broMode}
            onInitEditor={onInitEditor}
            onEditorEnabled={onEditorEnabled}
            onChange={onChange}
            onCommandsChanged={onCommandsChanged}
            setRef={setRef}
            notifyTooBigClipboard={notifyTooBigClipboard}
        />
    );
};

describe('src/components/editor', () => {
    let TouchEditor;
    let metrika;
    let CKEditor;
    let getData;
    let helpers;

    beforeEach(() => {
        jest.resetModules();
        jest.clearAllMocks();

        const { setTankerProjectId, addTranslation } = require('react-tanker');

        setTankerProjectId('yandex_disk_web');
        addTranslation('ru', require('../../../../../i18n/loc/ru'));
        listenersMap = {};
        TouchEditor = require('../../../../../src/components/editor/touch-editor').default;
        metrika = require('../../../../../src/helpers/metrika');
        CKEditor = require('@ckeditor/ckeditor5-react');
        getData = require('../../../../../src/components/editor/getData');
        helpers = require('../../../../../src/components/editor/helpers');
    });

    describe('desktop editor', () => {
        it('should call ref callback on mount', () => {
            mount(getComponent({}));
            expect(setRef).toBeCalled();
        });

        it('should call "init editor" action if editor is opened in bro-mode', () => {
            window.yandex = {
                notes: {}
            };

            const wrapper = mount(getComponent({ broMode: true }));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            expect(onInitEditor).toBeCalled();
            window.yandex = null;
        });

        it('should call metrika when there are old names of elements in editor`s content', () => {
            const wrapper = mount(getComponent({ children: [{ name: 'heading1' }] }));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            expect(popFnCalls(metrika.countObj)[0]).toEqual([{
                note: {
                    editor: { heading1: '1' }
                }
            }]);
        });

        it('should call an appropriate action on editor focus', () => {
            const wrapper = mount(getComponent({}));

            wrapper.find(CKEditor).props().onFocus();
            expect(onEditorFocus).toBeCalled();
        });

        it('should call "editor enabled" action if an editor has become enabled in bro-mode', () => {
            window.yandex = {
                notes: {}
            };

            const wrapper = mount(getComponent({ broMode: true, noteState: STATES.LOADING, editorDisabled: true }));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            wrapper.setProps({ state: STATES.LOADED, editorDisabled: false });
            wrapper.setState({ ckEditorInitialized: true });
            expect(wrapper.find(TouchEditor).exists()).toBe(false);
            expect(onEditorEnabled).toBeCalled();
            window.yandex = null;
        });

        it('should call metrika on an editor`s container click', () => {
            const wrapper = mount(getComponent({}));

            wrapper.find('.note-editor__editor-wrapper').simulate('click');
            expect(popFnCalls(metrika.countNote)[0]).toEqual(['editor', 'click']);
        });

        it('should call an appropriate action on commands` state change', () => {
            const wrapper = mount(getComponent({}));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            listenersMap['change:value']();
            expect(onCommandsChanged).toBeCalledTimes(1);
        });

        it('should call an appropriate action and metrika on paste length limit excess', () => {
            const wrapper = mount(getComponent({}));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            mockedEditor.fire('pasteInterrupted', { length: 999 });
            expect(notifyTooBigClipboard).toBeCalled();
            expect(popFnCalls(metrika.countError)[0]).toEqual([`more than ${MAX_NOTE_CONTENT_LENGTH} symbols in clipboard`, '999']);
        });

        it('should call an appropriate action on editor`s content change', () => {
            getData.mockImplementation(() => ({}));

            const wrapper = mount(getComponent({ children: [] }));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            listenersMap.change({});
            expect(onChange).toBeCalled();
        });

        it('should not call an action when editor`s content has not been changed', () => {
            getData.mockImplementation(() => ({ children: [] }));

            const wrapper = mount(getComponent({ children: [] }));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            listenersMap.change({});
            expect(onChange).not.toBeCalled();
        });

        it('should not call an action when editor is disabled', () => {
            const wrapper = mount(getComponent({ editorDisabled: true }));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            listenersMap.change({});
            expect(onChange).not.toBeCalled();
        });
    });

    describe('touch editor', () => {
        const getMockedTouchEditor = (innerHTML = '<p>&nbsp;</p>') => ({
            blur: jest.fn(),
            textContent: '',
            innerHTML
        });

        beforeAll(() => {
            global.IS_TOUCH = true;
        });

        afterAll(() => {
            global.IS_TOUCH = false;
        });

        it('should mount TouchEditor for touch devices', () => {
            const wrapper = mount(getComponent({}));

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            wrapper.setState({ ckEditorInitialized: true });
            expect(wrapper.find(TouchEditor).exists()).toBe(true);
            expect(wrapper.render()).toMatchSnapshot();
        });

        it('should call an appropriate handler when paste limit exceeded on touch devices', () => {
            const wrapper = mount(getComponent({}));
            const mockedTouchEditor = getMockedTouchEditor();

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            wrapper.setState({ ckEditorInitialized: true });
            wrapper.instance()._touchEditor = mockedTouchEditor;
            wrapper.find(TouchEditor).props().onPasteLimitExceeded();
            expect(mockedTouchEditor.blur).toBeCalled();
            expect(notifyTooBigClipboard).toBeCalled();
        });

        it('should process touch editor`s html on input change', () => {
            const wrapper = mount(getComponent({}));
            const mockedTouchEditor = getMockedTouchEditor();

            wrapper.find(CKEditor).props().onInit(mockedEditor);
            wrapper.setState({ ckEditorInitialized: true });
            wrapper.instance()._touchEditor = mockedTouchEditor;
            wrapper.find(TouchEditor).props().onInput();
            expect(helpers.processTouchEditorHtmlContent).toBeCalled();
        });
    });
});
