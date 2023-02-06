import {
    openDialog,
    closeDialog,
    fetchNoteContent,
    updateCurrent,
    createNote,
    deleteCurrent,
    togglePin,
    updateNoteTitle,
    initEditor,
    setBlockNoteSelection,
    pasteHtmlOnNoteLoaded,
    updateWindowWidth
} from '../../../../../src/store/actions';

import {
    UPDATE_CURRENT_NOTE,
    DELETE_CURRENT_NOTE,
    OPEN_DIALOG,
    CLOSE_DIALOG,
    INIT_EDITOR,
    SET_BLOCK_NOTE_SELECTION,
    PASTE_HTML_ON_NOTE_LOADED,
    UPDATE_WINDOW_WIDTH
} from '../../../../../src/store/types';

jest.mock('../../../../../src/store/actions/common', () => ({
    fetchCloudApi: jest.fn(),
    updateCurrent: jest.fn(),
    updateNote: jest.fn(),
    addNote: jest.fn(),
    checkNoteExist: jest.fn()
}));
import { fetchCloudApi, updateNote, addNote, checkNoteExist } from '../../../../../src/store/actions/common';
import { PIN_TAG_CODE, STATES, DESKTOP_LAYOUT_THRESHOLD } from '../../../../../src/consts';

describe('store/actions/index =>', () => {
    const dispatch = jest.fn((arg) => typeof arg === 'function' ? arg(dispatch) : arg);
    beforeEach(() => {
        jest.clearAllMocks();
    });
    it('openDialog => should call OPEN_DIALOG', () => {
        expect(openDialog('dialog-name', { key: 'value' })).toEqual({
            type: OPEN_DIALOG,
            payload: {
                name: 'dialog-name',
                data: { key: 'value' }
            }
        });
    });

    it('closeDialog => should call CLOSE_DIALOG', () => {
        expect(closeDialog('dialog-name')).toEqual({
            type: CLOSE_DIALOG,
            payload: {
                name: 'dialog-name'
            }
        });
    });

    describe('fetchNoteContent =>', () => {
        it('should update note to LOADING and update note to LOADED after request successfully ends', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.resolve({
                revision: 123,
                data: {
                    name: '$root',
                    children: [{
                        name: 'paragraph'
                    }]
                }
            }));
            fetchNoteContent('note-id')(dispatch).then(() => {
                expect(dispatch).toBeCalledTimes(3);
                expect(updateNote).toBeCalledTimes(2);
                const updateNoteCalls = popFnCalls(updateNote);
                expect(updateNoteCalls[0]).toEqual([{
                    id: 'note-id',
                    content: {
                        state: STATES.LOADING
                    }
                }]);
                expect(fetchCloudApi).toBeCalled();
                expect(popFnCalls(fetchCloudApi)[0]).toEqual(['notes/notes/note-id/content']);
                expect(updateNoteCalls[1]).toEqual([{
                    id: 'note-id',
                    content: {
                        revision: 123,
                        state: STATES.LOADED,
                        data: {
                            name: '$root',
                            children: [{
                                name: 'paragraph'
                            }]
                        }
                    }
                }]);
                done();
            });
        });

        it('should update note to LOADING and update note to ERROR after request fails', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.reject('rejected!'));
            fetchNoteContent('note-id')(dispatch).then(() => {
                expect(dispatch).toBeCalledTimes(3);
                expect(updateNote).toBeCalledTimes(2);
                const updateNoteCalls = popFnCalls(updateNote);
                expect(updateNoteCalls[0]).toEqual([{
                    id: 'note-id',
                    content: {
                        state: STATES.LOADING
                    }
                }]);
                expect(fetchCloudApi).toBeCalled();
                expect(popFnCalls(fetchCloudApi)[0]).toEqual(['notes/notes/note-id/content']);
                expect(updateNoteCalls[1]).toEqual([{
                    id: 'note-id',
                    error: 'rejected!',
                    content: {
                        state: STATES.ERROR
                    }
                }]);
                done();
            });
        });
    });

    it('updateCurrent => should call UPDATE_CURRENT_NOTE', () => {
        expect(updateCurrent('note-id')).toEqual({
            type: UPDATE_CURRENT_NOTE,
            payload: 'note-id'
        });
    });

    describe('createNote =>', () => {
        it('should call API, add note and update current if request ends successfully', (done) => {
            fetchCloudApi.mockImplementation((path, { body }) => Promise.resolve({ data: Object.assign({ id: 'new-note' }, JSON.parse(body)) }));
            createNote({
                title: 'note-title',
                snippet: 'note-snippet',
                tags: {}
            })(dispatch).then(() => {
                expect(dispatch).toBeCalledTimes(3);
                expect(fetchCloudApi).toBeCalled();
                expect(addNote).toBeCalled();
                expect(popFnCalls(addNote)[0]).toEqual([{
                    id: 'new-note',
                    title: 'note-title',
                    snippet: 'note-snippet',
                    tags: {}
                }]);
                expect(popFnCalls(dispatch)[2]).toEqual([{
                    type: UPDATE_CURRENT_NOTE,
                    payload: 'new-note'
                }]);
                done();
            });
        });
        it('should call API, do not add note and do not update current if request fails', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.reject());
            createNote({
                title: 'note-title',
                snippet: 'note-snippet',
                tags: {}
            })(dispatch).catch(() => {
                expect(dispatch).toBeCalledTimes(1);
                expect(fetchCloudApi).toBeCalled();
                expect(addNote).not.toBeCalled();
                done();
            });
        });
        it('should use default parameters', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.resolve({ data: {} }));
            createNote()(dispatch).then(() => {
                expect(dispatch).toBeCalledTimes(3);
                expect(fetchCloudApi).toBeCalled();
                expect(popFnCalls(fetchCloudApi)[0]).toEqual([
                    'notes/notes',
                    {
                        method: 'POST',
                        body: '{"title":"","snippet":"","tags":[]}',
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    }
                ]);
                expect(addNote).toBeCalled();
                done();
            });
        });
    });

    describe('deleteCurrent =>', () => {
        const mockedGetState = () => ({ notes: {} });
        it('should call api and DELETE_CURRENT_NOTE if note exists', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.resolve());
            checkNoteExist.mockImplementation(() => true);
            deleteCurrent()(dispatch, mockedGetState).then(() => {
                expect(dispatch).toBeCalledTimes(3);
                expect(fetchCloudApi).toBeCalled();
                expect(popFnCalls(dispatch)[2]).toEqual([{
                    type: DELETE_CURRENT_NOTE
                }]);
                done();
            });
        });
        it('should call api and nothing else if note does not exist', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.resolve());
            checkNoteExist.mockImplementation(() => false);
            deleteCurrent()(dispatch, mockedGetState).then(() => {
                expect(dispatch).toBeCalledTimes(1);
                expect(fetchCloudApi).toBeCalled();
                done();
            });
        });
    });

    describe('togglePin', () => {
        fetchCloudApi.mockImplementation(() => Promise.resolve());
        it('should call api with add_tags and update note if note is not pinned', (done) => {
            const mockedGetState = () => ({
                notes: {
                    current: 'note-id',
                    notes: {
                        'note-id': {
                            tags: {}
                        }
                    }
                }
            });
            togglePin()(dispatch, mockedGetState).then(() => {
                expect(dispatch).toBeCalledTimes(3);
                expect(fetchCloudApi).toBeCalled();
                expect(popFnCalls(fetchCloudApi)[0]).toEqual([
                    'notes/notes/note-id',
                    {
                        method: 'PATCH',
                        body: `{"add_tags":[${PIN_TAG_CODE}]}`,
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    }
                ]);
                expect(updateNote).toBeCalled();
                expect(popFnCalls(updateNote)[0]).toEqual([{
                    id: 'note-id',
                    tags: {
                        pin: true
                    }
                }]);
                done();
            });
        });
    });
    it('should call api with remove_tags and update note if note is pinned', (done) => {
        const mockedGetState = () => ({
            notes: {
                current: 'note-id',
                notes: {
                    'note-id': {
                        tags: {
                            pin: true
                        }
                    }
                }
            }
        });
        togglePin()(dispatch, mockedGetState).then(() => {
            expect(dispatch).toBeCalledTimes(3);
            expect(fetchCloudApi).toBeCalled();
            expect(popFnCalls(fetchCloudApi)[0]).toEqual([
                'notes/notes/note-id',
                {
                    method: 'PATCH',
                    body: `{"remove_tags":[${PIN_TAG_CODE}]}`,
                    headers: {
                        'Content-Type': 'application/json'
                    }
                }
            ]);
            expect(updateNote).toBeCalled();
            expect(popFnCalls(updateNote)[0]).toEqual([{
                id: 'note-id',
                tags: {
                    pin: false
                }
            }]);
            done();
        });
    });

    describe('updateNoteTitle', () => {
        it('should call api and update note if request ends successfully', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.resolve());
            updateNoteTitle('id', 'title', 'snippet')(dispatch).then(() => {
                expect(dispatch).toBeCalledTimes(4);
                expect(fetchCloudApi).toBeCalled();
                expect(updateNote).toBeCalledTimes(2);
                const updateNoteCalls = popFnCalls(updateNote);
                expect(updateNoteCalls[0]).toEqual([{
                    id: 'id',
                    saving: true
                }]);
                expect(updateNoteCalls[1]).toEqual([{
                    id: 'id',
                    title: 'title',
                    snippet: 'snippet',
                    saving: false
                }]);
                done();
            });
        });
        it('should call api and update note with error if request fails', (done) => {
            fetchCloudApi.mockImplementation(() => Promise.reject('rejected!'));
            updateNoteTitle('id', 'title', 'snippet')(dispatch).then(() => {
                expect(dispatch).toBeCalledTimes(4);
                expect(fetchCloudApi).toBeCalled();
                expect(updateNote).toBeCalledTimes(2);
                const updateNoteCalls = popFnCalls(updateNote);
                expect(updateNoteCalls[0]).toEqual([{
                    id: 'id',
                    saving: true
                }]);
                expect(updateNoteCalls[1]).toEqual([{
                    id: 'id',
                    saving: false
                }]);
                done();
            });
        });
    });

    it('initEditor => should call INIT_EDITOR', () => {
        const editor = {};

        expect(initEditor(editor)).toEqual({
            type: INIT_EDITOR,
            payload: editor
        });
    });

    it('setBlockNoteSelection => should call SET_BLOCK_NOTE_SELECTION', () => {
        const shouldBlockNoteSelection = true;

        expect(setBlockNoteSelection(shouldBlockNoteSelection)).toEqual({
            type: SET_BLOCK_NOTE_SELECTION,
            payload: shouldBlockNoteSelection
        });
    });

    it('pasteHtmlOnNoteLoaded => should call PASTE_HTML_ON_NOTE_LOADED', () => {
        const rawHtml = 'test';

        expect(pasteHtmlOnNoteLoaded(rawHtml)).toEqual({
            type: PASTE_HTML_ON_NOTE_LOADED,
            payload: rawHtml
        });
    });

    it('updateWindowWidth => should call UPDATE_WINDOW_WIDTH', () => {
        const originalWindowInnerWidth = window.innerWidth;

        window.innerWidth = DESKTOP_LAYOUT_THRESHOLD;
        expect(updateWindowWidth()).toEqual({
            type: UPDATE_WINDOW_WIDTH,
            payload: DESKTOP_LAYOUT_THRESHOLD
        });
        window.innerWidth = originalWindowInnerWidth;
    });
});
