jest.mock('../../../../../src/store/actions', () => ({
    updateCurrent: jest.fn()
}));
jest.mock('../../../../../src/store/actions/database', () => ({
    initNotesDatabase: jest.fn(() => Promise.resolve())
}));
jest.mock('../../../../../src/store/actions/yabro', () => ({
    createNoteOnReceiveHtmlFromYaBro: jest.fn()
}));
jest.mock('../../../../../src/store/actions/common', () => ({
    fetchCloudApi: jest.fn(() => Promise.resolve())
}));
jest.mock('../../../../../src/store/selectors', () => ({
    getSortedNoteList: jest.fn(() => [{ id: 'note-1' }, { id: 'note-2' }, { id: 'note-3' }])
}));

import { initNotesDatabase } from '../../../../../src/store/actions/database';
import { updateCurrent } from '../../../../../src/store/actions';
import { fetchCloudApi } from '../../../../../src/store/actions/common';
import { createNoteOnReceiveHtmlFromYaBro } from '../../../../../src/store/actions/yabro';
import { initNotesList } from '../../../../../src/store/actions/init';

describe('initNotesList =>', () => {
    const newNoteId = '123';
    const noteIdFromLocalStorage = '321';
    const getMockedGetState = ({
        broMode = false,
        balloonMode = false,
        notes = {}
    }) => () => ({
        environment: { broMode, balloonMode, currentLang: 'ru' },
        notes: { notes }
    });
    const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch, getMockedGetState({})) : arg);
    const setYaNotesApiSupport = (shouldSupportApi) => {
        if (shouldSupportApi) {
            window.yandex = { notes: {} };
        } else {
            window.yandex = undefined;
        }
    };
    const setLocalStorageSupport = (shouldSupportLocalStorage) => {
        if (shouldSupportLocalStorage) {
            window.localStorage = {
                getItem: jest.fn(() => noteIdFromLocalStorage),
                setItem: jest.fn()
            };
        } else {
            window.localStorage = undefined;
        }
    };

    createNoteOnReceiveHtmlFromYaBro.mockImplementation(() => Promise.resolve(({ newNoteId })));

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should fetch notes and focus on the last edited note (or first pinned note if it exists) if neither bro-mode nor balloon-mode is supported', (done) => {
        initNotesList()(dispatch, getMockedGetState({})).then(() => {
            expect(fetchCloudApi).toBeCalledTimes(1);
            expect(initNotesDatabase).toBeCalledTimes(1);
            expect(popFnCalls(updateCurrent)[0]).toEqual(['note-1']);
            done();
        });
    });

    it('should fetch notes, call createNoteOnReceiveHtmlFromYaBro and focus on a created note if the bro-mode is supported', (done) => {
        setYaNotesApiSupport(true);
        initNotesList()(dispatch, getMockedGetState({ broMode: true })).then(() => {
            expect(fetchCloudApi).toBeCalledTimes(1);
            expect(initNotesDatabase).toBeCalledTimes(1);
            expect(createNoteOnReceiveHtmlFromYaBro).toBeCalledTimes(1);
            expect(popFnCalls(updateCurrent)[0]).toEqual([newNoteId]);
            setYaNotesApiSupport(false);
            done();
        });
    });

    it('should fetch notes and focus on the last visited note if it is in localStorage and if the balloon-mode is supported', (done) => {
        setLocalStorageSupport(true);
        initNotesList()(dispatch, getMockedGetState({
            balloonMode: true,
            notes: { [noteIdFromLocalStorage]: { id: noteIdFromLocalStorage } }
        })).then(() => {
            expect(fetchCloudApi).toBeCalledTimes(1);
            expect(initNotesDatabase).toBeCalledTimes(1);
            expect(popFnCalls(updateCurrent)[0]).toEqual([noteIdFromLocalStorage]);
            setLocalStorageSupport(false);
            done();
        });
    });

    it('should fetch notes without focus if note`s id is not in localStorage or if note with this id was deleted and if the balloon-mode is supported', (done) => {
        setLocalStorageSupport(true);
        initNotesList()(dispatch, getMockedGetState({ balloonMode: true })).then(() => {
            expect(fetchCloudApi).toBeCalledTimes(1);
            expect(initNotesDatabase).toBeCalledTimes(1);
            expect(updateCurrent).not.toBeCalled();
            expect(window.localStorage.setItem).toBeCalledTimes(1);
            setLocalStorageSupport(false);
            done();
        });
    });

    it('should fetch notes, call createNoteOnReceiveHtmlFromYaBro and focus on a newly created note if the bro-mode and balloon-mode are supported', (done) => {
        setYaNotesApiSupport(true);
        setLocalStorageSupport(true);
        initNotesList()(dispatch, getMockedGetState({
            broMode: true,
            balloonMode: true,
            notes: { [newNoteId]: { id: newNoteId } }
        })).then(() => {
            expect(fetchCloudApi).toBeCalledTimes(1);
            expect(initNotesDatabase).toBeCalledTimes(1);
            expect(createNoteOnReceiveHtmlFromYaBro).toBeCalledTimes(1);
            expect(popFnCalls(updateCurrent)[0]).toEqual([newNoteId]);
            setYaNotesApiSupport(false);
            setLocalStorageSupport(false);
            done();
        });
    });
});
