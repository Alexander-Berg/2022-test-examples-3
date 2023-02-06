jest.mock('../../../../../src/store/selectors', () => ({
    getSortedNoteList: jest.fn()
}));
jest.mock('../../../../../src/store/actions/common', () => ({
    handleError: jest.fn(),
    addNote: jest.fn()
}));
jest.mock('../../../../../src/store/actions', () => ({
    createNote: jest.fn(() => Promise.resolve()),
    callCreateNoteApi: jest.fn(),
    setBlockNoteSelection: jest.fn(),
    pasteHtmlOnNoteLoaded: jest.fn()
}));
jest.mock('../../../../../src/store/actions/attachments', () => ({
    toggleSlider: jest.fn()
}));

import {
    createNote,
    callCreateNoteApi,
    setBlockNoteSelection,
    pasteHtmlOnNoteLoaded
} from '../../../../../src/store/actions';
import { handleError, addNote } from '../../../../../src/store/actions/common';
import { createNoteOnReceiveHtmlFromYaBro } from '../../../../../src/store/actions/yabro';
import { toggleSlider } from '../../../../../src/store/actions/attachments';
import { getSortedNoteList } from '../../../../../src/store/selectors';
import { MAX_NOTES_COUNT, MAX_NOTE_CONTENT_LENGTH, ERRORS } from '../../../../../src/consts';

describe('createNoteOnReceiveHtmlFromYaBro =>', () => {
    const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch) : arg);
    const setYaNotesApi = (rawHtml) => {
        window.yandex = {
            notes: {
                getSelectedText: jest.fn((callback) => setTimeout(() => callback(rawHtml), 0))
            }
        };
    };
    const mockedGetState = () => ({
        notes: {
            notes: {}
        }
    });
    const rawHtml = '<span>text</span>';
    const newNote = {
        data: { id: '123' }
    };

    callCreateNoteApi.mockImplementation(() => Promise.resolve(newNote));
    getSortedNoteList.mockImplementation(() => []);

    beforeEach(() => {
        jest.clearAllMocks();
        setYaNotesApi(rawHtml);
    });
    afterEach(() => {
        window.yandex = undefined;
    });

    it('should dispatch callCreateNoteApi action if option.addToStoreAndFocusOnNote === false', (done) => {
        createNoteOnReceiveHtmlFromYaBro({ addToStoreAndFocusOnNote: false })(dispatch, mockedGetState)
            .then((response) => {
                expect(createNote).not.toBeCalled();
                expect(callCreateNoteApi).toBeCalledTimes(1);
                expect(addNote).toBeCalledTimes(1);
                expect(popFnCalls(addNote)[0]).toEqual([newNote.data]);
                expect(popFnCalls(setBlockNoteSelection)[0]).toEqual([true]);
                expect(popFnCalls(pasteHtmlOnNoteLoaded)[0]).toEqual([rawHtml]);
                expect(window.yandex.notes.getSelectedText).toBeCalled();
                expect(response).toEqual({ newNoteId: newNote.data.id });
                done();
            });
    });

    it('should dispatch creatNote action if option.addToStoreAndFocusOnNote === true', (done) => {
        createNoteOnReceiveHtmlFromYaBro({ addToStoreAndFocusOnNote: true })(dispatch, mockedGetState)
            .then((response) => {
                expect(createNote).toBeCalledTimes(1);
                expect(callCreateNoteApi).not.toBeCalled();
                expect(popFnCalls(setBlockNoteSelection)[0]).toEqual([true]);
                expect(popFnCalls(pasteHtmlOnNoteLoaded)[0]).toEqual([rawHtml]);
                expect(response).toEqual({ newNoteId: undefined });
                done();
            });
    });

    it('should exit slider if it has been opened when createNoteOnReceiveHtmlFromYaBro was called', (done) => {
        setYaNotesApi();
        createNoteOnReceiveHtmlFromYaBro({ addToStoreAndFocusOnNote: false })(
            dispatch,
            () => ({ notes: { sliderResourceId: '123' } })
        ).then(() => {
            expect(popFnCalls(toggleSlider)[0]).toEqual([]);
            done();
        });
    });

    it('should show error TOO_MANY_NOTES notification if a number of user notes >= MAX_NOTES_COUNT', (done) => {
        getSortedNoteList.mockImplementationOnce(() => new Array(MAX_NOTES_COUNT));
        createNoteOnReceiveHtmlFromYaBro({ addToStoreAndFocusOnNote: false })(dispatch, mockedGetState)
            .then(() => {
                expect(handleError).toBeCalledTimes(1);
                expect(popFnCalls(handleError)[0]).toEqual([ERRORS.TOO_MANY_NOTES]);
                done();
            });
    });

    it('should show error TOO_BIG_CLIPBOARD_TO_PASTE if YaBro selected string length > MAX_NOTE_CONTENT_LENGTH', (done) => {
        const rawHtml = '.'.repeat(MAX_NOTE_CONTENT_LENGTH + 1);

        setYaNotesApi(rawHtml);
        createNoteOnReceiveHtmlFromYaBro({ addToStoreAndFocusOnNote: false })(dispatch, mockedGetState)
            .then(() => {
                expect(handleError).toBeCalledTimes(1);
                expect(popFnCalls(handleError)[0]).toEqual([ERRORS.TOO_BIG_CLIPBOARD_TO_PASTE]);
                done();
            });
    });
});
