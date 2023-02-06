import NotesList, { countOffline } from '../../../../../src/components/notes-list';

import React from 'react';
import { mount } from 'enzyme';
import getStore from '../../../../../src/store';
import { Provider } from 'react-redux';
import CreateNoteButton from '../../../../../src/components/create-note-button';
import { STATES, MAX_NOTES_COUNT, ERRORS } from '../../../../../src/consts';

jest.mock('../../../../../src/components/notes-slider', () => () => null);
jest.mock('../../../../../src/store/actions/init', () => ({
    initNotesList: jest.fn(() => ({ type: '' }))
}));
import { initNotesList } from '../../../../../src/store/actions/init';

jest.mock('../../../../../src/store/actions', () => ({
    updateCurrent: jest.fn(() => ({ type: '' })),
    createNote: jest.fn()
}));
import { updateCurrent, createNote } from '../../../../../src/store/actions';

jest.mock('../../../../../src/store/actions/common', () => ({
    handleError: jest.fn(() => ({ type: '' }))
}));
import { handleError } from '../../../../../src/store/actions/common';

jest.mock('../../../../../src/helpers/metrika', () => ({
    count: jest.fn()
}));
import { count } from '../../../../../src/helpers/metrika';

jest.mock('@ps-int/ufo-rocks/lib/helpers/offline');
import MockedOfflineMonitor from '@ps-int/ufo-rocks/lib/helpers/offline';

const getInitialState = (current) => ({
    notes: {
        state: STATES.LOADED,
        current,
        notes: {
            'note-1': {
                id: 'note-1',
                title: 'created today',
                snippet: 'note 1 snippet',
                mtime: '2019-04-21T17:43:05.219Z',
                tags: {},
                attachments: {
                    attach: {
                        preview: 'attach-preview-url'
                    }
                },
                attachmentOrder: ['attach']
            },
            'note-2': {
                id: 'note-2',
                title: 'created yesterday and pinned',
                snippet: 'note 2 snippet',
                mtime: '2019-04-20T15:21:02.000Z',
                tags: {
                    pin: true
                },
                attachments: {},
                attachmentOrder: []
            },
            'note-3': {
                id: 'note-3',
                title: 'created 6 days ago',
                snippet: 'note 3 snippet',
                mtime: '2019-04-15T12:34:00.000Z',
                tags: {},
                attachments: {},
                attachmentOrder: []
            },
            'note-4': {
                id: 'note-4',
                title: 'created 3 days ago and deleted',
                snippet: 'note 4 snippet',
                mtime: '2019-04-18T06:15:00.123Z',
                tags: {
                    deleted: true
                },
                attachments: {},
                attachmentOrder: []
            },
            'empty-note': {
                id: 'empty-note',
                title: '', // created a week ago and it is empty
                snippet: '',
                mtime: '2019-04-14T09:40:00.000Z',
                tags: {},
                attachments: {},
                attachmentOrder: []
            }
        }
    }
});

const generateStateWithNotes = (notesNumber = MAX_NOTES_COUNT) => {
    const state = {
        notes: {
            state: STATES.LOADED,
            notes: {}
        }
    };
    for (let i = notesNumber; i > 0; i--) {
        state.notes.notes[`note-${i}`] = {
            id: `note-${i}`,
            title: `Note ${i} title`,
            snippet: `Note ${i} snippet`,
            tags: {},
            attachments: {},
            attachmentOrder: []
        };
    }

    return state;
};

const forceEditorFocus = jest.fn();
const getComponent = (store) => (
    <Provider store={store}>
        <NotesList
            forceEditorFocus={forceEditorFocus}
        />
    </Provider>
);

describe('components/notes-list =>', () => {
    const OriginalDate = global.Date;
    const mockedDate = '2019-04-21T18:09:29.742Z';
    beforeAll(() => {
        global.Date = function(arg) {
            // mock `new Date()` because date format in list depends on time from note creation till now
            return arg && new OriginalDate(arg) || new OriginalDate(mockedDate);
        };
        global.Date.now = () => Number(new OriginalDate(mockedDate));
    });
    afterAll(() => {
        global.Date = OriginalDate;
        jest.resetAllMocks();
    });
    beforeEach(() => {
        jest.clearAllMocks();
    });
    let wrapper;
    afterEach(() => {
        wrapper && wrapper.unmount();
        wrapper = null;
    });

    it('should show spin and call initNotesList when just mounted', () => {
        wrapper = mount(getComponent(getStore()));
        expect(wrapper.render()).toMatchSnapshot();
        expect(initNotesList).toBeCalled();
    });

    it('should show list', () => {
        wrapper = mount(getComponent(getStore(getInitialState())));
        expect(wrapper.render()).toMatchSnapshot();
    });

    it('should call createNote and count `new note -> click` on create note button click', () => {
        createNote.mockImplementation(() => () => Promise.resolve());

        wrapper = mount(getComponent(getStore(getInitialState())));
        wrapper.find(CreateNoteButton).simulate('click');
        expect(createNote).toBeCalled();
        expect(count).toBeCalled();
        expect(popFnCalls(count)[0]).toEqual(['new note', 'click']);
    });

    it('should not call createNote and should count `current is empty` on create note button click if current note is empty', () => {
        wrapper = mount(getComponent(getStore(getInitialState('empty-note'))));
        wrapper.find(CreateNoteButton).simulate('click');
        expect(createNote).not.toBeCalled();
        expect(count).toBeCalled();
        expect(popFnCalls(count)[0]).toEqual(['new note', 'click', 'current is empty']);
        expect(forceEditorFocus).toBeCalled();
    });

    it('should not call createNote and should count `already creating` on create note button click if note already creating', () => {
        const creatingPromise = new Promise((resolve) => {
            setTimeout(() => resolve(), 50);
        });
        createNote.mockImplementation(() => () => creatingPromise);

        wrapper = mount(getComponent(getStore(getInitialState())));
        wrapper.find(CreateNoteButton).simulate('click');
        expect(createNote).toBeCalled();
        createNote.mockClear();
        wrapper.find(CreateNoteButton).simulate('click');
        expect(createNote).not.toBeCalled();
        expect(count).toBeCalled();
        const countCalls = popFnCalls(count);
        expect(countCalls.length).toEqual(2);
        expect(countCalls[0]).toEqual(['new note', 'click']);
        expect(countCalls[1]).toEqual(['new note', 'click', 'already creating']);
    });

    it('should call updateCurrent and count `list -> click` on note click', () => {
        wrapper = mount(getComponent(getStore(getInitialState())));
        const thirdNote = wrapper
            .find('.notes-list-item').at(2);

        thirdNote.simulate('click');
        expect(updateCurrent).toBeCalled();
        expect(popFnCalls(updateCurrent)[0]).toEqual(['note-3', true]);
        expect(count).toBeCalled();
        expect(popFnCalls(count)[0]).toEqual(['list', 'click']);
    });

    it('should count offline', () => {
        const mockedOfflineMonitor = new MockedOfflineMonitor();
        wrapper = mount(getComponent(getStore(getInitialState())));
        mockedOfflineMonitor.callOfflineHandlers();
        mockedOfflineMonitor.callOnlineHandlers();
        expect(count).toBeCalled();
        expect(popFnCalls(count)[0]).toEqual(['offline', '< 1 second']);
    });

    describe('countOffline =>', () => {
        it('should count few days offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setDate(date.getDate() - 3);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '3 days']);
        });
        it('should count 1 day offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setDate(date.getDate() - 1);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '1 days']);
        });
        it('should count few hours offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setHours(date.getHours() - 3);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '3 hours']);
        });
        it('should count 1 hour offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setHours(date.getHours() - 1);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '1 hours']);
        });
        it('should count few minutes offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setMinutes(date.getMinutes() - 3);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '3 minutes']);
        });
        it('should count 1 minute offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setMinutes(date.getMinutes() - 1);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '1 minutes']);
        });
        it('should count few seconds offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setSeconds(date.getSeconds() - 3);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '3 seconds']);
        });
        it('should count 1 second offline', () => {
            const date = new OriginalDate(mockedDate);
            date.setSeconds(date.getSeconds() - 1);
            countOffline(date);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '1 seconds']);
        });
        it('should count < 1 second offline', () => {
            countOffline(mockedDate);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['offline', '< 1 second']);
        });
    });

    describe('on touch devices', () => {
        const originalIsTouch = global.IS_TOUCH;

        beforeAll(() => {
            global.IS_TOUCH = true;
        });

        afterAll(() => {
            global.IS_TOUCH = originalIsTouch;
        });

        it('should show notes list and `create note` button on touch devices', () => {
            wrapper = mount(getComponent(getStore(getInitialState())));
            expect(wrapper.render()).toMatchSnapshot();
        });

        it('should call createNote and count `new note -> click` on create note button click on touch devices', () => {
            createNote.mockImplementation(() => () => Promise.resolve());

            wrapper = mount(getComponent(getStore(getInitialState())));
            wrapper.find(CreateNoteButton).simulate('click');
            expect(createNote).toBeCalled();
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['new note', 'click']);
        });

        it('should have `create note` floating action button on touch devices', () => {
            wrapper = mount(getComponent(getStore(getInitialState())));

            const createNoteButtonDesktop = wrapper
                .find('lego-components_button')
                .filterWhere((button) => /create-note-button_desktop$/.test(button.props().className));
            const createNoteButtonTouch = wrapper
                .find('lego-components_button')
                .filterWhere((button) => /create-note-button_touch$/.test(button.props().className));

            expect(createNoteButtonDesktop.exists()).toBe(false);
            expect(createNoteButtonTouch.exists()).toBe(true);
        });

        it('should call updateCurrent, count `list -> click` after note click on touch device', () => {
            wrapper = mount(getComponent(getStore(getInitialState())));

            const thirdNote = wrapper.find('.notes-list-item').at(2);

            thirdNote.simulate('click');
            expect(updateCurrent).toBeCalled();
            expect(popFnCalls(updateCurrent)[0]).toEqual(['note-3', true]);
            expect(count).toBeCalled();
            expect(popFnCalls(count)[0]).toEqual(['list', 'click']);
        });

        it('should show `too many notes` notification on `create note` button click on touch devices if notes` limit is exceeded', () => {
            wrapper = mount(getComponent(getStore(generateStateWithNotes())));
            wrapper.find(CreateNoteButton).simulate('click');
            expect(handleError).toBeCalledWith(ERRORS.TOO_MANY_NOTES);
        });

        it('should update scrollTop of document.scrollingElement if the opened notes` list has been updated', () => {
            const mockedWindowYOffset = 10;
            const originalWindowYOffset = window.pageYOffset;
            const originalScrollingElement = document.scrollingElement;

            window.pageYOffset = mockedWindowYOffset;
            document.scrollingElement = {};
            wrapper = mount(getComponent(getStore(getInitialState())));
            expect(document.scrollingElement).toEqual({});
            wrapper.find(NotesList).instance().forceUpdate();
            expect(window.pageYOffset).toBe(mockedWindowYOffset);
            expect(document.scrollingElement).toEqual({ scrollTop: mockedWindowYOffset });
            window.pageYOffset = originalWindowYOffset;
            document.scrollingElement = originalScrollingElement;
        });

        it('should not update scrollTop of document.scrollingElement if the note is opened', () => {
            const mockedWindowYOffset = 10;
            const originalWindowYOffset = window.pageYOffset;
            const originalScrollingElement = document.scrollingElement;

            window.pageYOffset = mockedWindowYOffset;
            document.scrollingElement = {};
            wrapper = mount(getComponent(getStore(getInitialState('note-1'))));
            expect(document.scrollingElement).toEqual({});
            wrapper.find(NotesList).instance().forceUpdate();
            expect(window.pageYOffset).toBe(mockedWindowYOffset);
            expect(document.scrollingElement).toEqual({});
            window.pageYOffset = originalWindowYOffset;
            document.scrollingElement = originalScrollingElement;
        });
    });
});
