import { getCloudApiBaseUrl, handleError, fetchCloudApi, checkNoteExist, updateNote, addNote } from '../../../../../src/store/actions/common';
import { ERRORS, STATES, HTTP_STATUSES } from '../../../../../src/consts';
import { ADD_NOTE, UPDATE_NOTE } from '../../../../../src/store/types';

jest.mock('@ps-int/ufo-rocks/lib/store/actions/notifications', () => ({
    clearAllAndNotify: jest.fn(),
    notify: jest.fn()
}));
jest.mock('../../../../../src/store/actions/', () => ({
    unauthAndReloadPage: jest.fn()
}));
import { clearAllAndNotify, notify } from '@ps-int/ufo-rocks/lib/store/actions/notifications';
import { unauthAndReloadPage } from '../../../../../src/store/actions/';

const cloudApiOrigin = 'https://cloud-api.yandex.net';

describe('store/actions/common =>', () => {
    const userUid = '001';
    const defaultGetState = () => ({
        user: { id: userUid },
        services: {
            'cloud-api': cloudApiOrigin
        },
        notifications: {
            current: null
        }
    });
    const dispatch = jest.fn(
        (arg) => typeof arg === 'function' ?
            arg(dispatch, defaultGetState) :
            undefined
    );
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('getCloudApiBaseUrl', () => {
        expect(getCloudApiBaseUrl({
            services: {
                'cloud-api': cloudApiOrigin
            }
        })).toEqual(`${cloudApiOrigin}/yadisk_web/`);
    });

    describe('handleError =>', () => {
        it('should do nothing if current notification is eternal', () => {
            const stateWithEternalNotification = {
                notifications: {
                    current: {
                        isEternal: true
                    }
                }
            };
            handleError('error')(dispatch, () => stateWithEternalNotification);
            expect(dispatch).not.toBeCalled();
        });
        it('should clearAllAndNotify if lost internet connection', () => {
            handleError(ERRORS.CONNECTION_ERROR)(dispatch, defaultGetState);
            expect(dispatch).toBeCalled();
            expect(clearAllAndNotify).toBeCalled();
            expect(popFnCalls(clearAllAndNotify)[0]).toEqual([{ text: ERRORS.CONNECTION_ERROR, isEternal: true }]);
        });
        it('should call notify if online', () => {
            handleError(ERRORS.NOT_AN_IMAGE)(dispatch, defaultGetState);
            expect(dispatch).toBeCalled();
            expect(notify).toBeCalled();
            expect(popFnCalls(notify)[0]).toEqual([{ text: ERRORS.NOT_AN_IMAGE }]);
        });
    });

    describe('fetchCloudApi =>', () => {
        const originalFetch = global.fetch;
        beforeAll(() => {
            global.fetch = jest.fn();
        });
        afterAll(() => {
            global.fetch = originalFetch;
        });

        it('should call fetch', () => {
            global.fetch.mockImplementation(() => Promise.resolve({}));
            fetchCloudApi('some/path')(dispatch, defaultGetState);
            expect(global.fetch).toBeCalled();
            expect(popFnCalls(global.fetch)[0]).toEqual([
                `${cloudApiOrigin}/yadisk_web/v1/some/path`,
                {
                    credentials: 'include',
                    headers: { 'X-Uid': userUid }
                }
            ]);
        });

        it('should reject if request does not return ok', (done) => {
            global.fetch.mockImplementation(() => Promise.resolve({ key: 'value' }));
            fetchCloudApi('some/path')(dispatch, defaultGetState).catch((error) => {
                expect(error).toEqual({ key: 'value' });
                done();
            });
        });

        it('should return object with only revision for 204', (done) => {
            global.fetch.mockImplementation(() => Promise.resolve({
                ok: true,
                status: 204,
                headers: {
                    get: (headerName) => headerName === 'X-Actual-Revision' ? 123 : undefined
                }
            }));
            fetchCloudApi('some/path')(dispatch, defaultGetState).then((res) => {
                expect(res).toEqual({ revision: 123 });
                done();
            });
        });

        it('should return object with revision and data for 200', (done) => {
            global.fetch.mockImplementation(() => Promise.resolve({
                ok: true,
                status: 200,
                headers: {
                    get: (headerName) => headerName === 'X-Actual-Revision' ? 456 : undefined
                },
                json: () => Promise.resolve({ key1: 'value1', key2: 'value2' })
            }));
            fetchCloudApi('some/path')(dispatch, defaultGetState)
                .then((res) => {
                    expect(res).toEqual({ revision: 456, data: { key1: 'value1', key2: 'value2' } });
                    done();
                });
        });

        it('should call unauthAndReloadPage and reload page if response code is 401 (UNAUTHORIZED)', (done) => {
            const response = { ok: false, status: HTTP_STATUSES.UNAUTHORIZED };

            global.fetch.mockImplementation(() => Promise.reject(response));
            fetchCloudApi('some/path')(dispatch, defaultGetState)
                .then((res) => {
                    expect(res).toEqual(undefined);
                    expect(unauthAndReloadPage).toBeCalled();
                    expect(notify).not.toBeCalled();
                    done();
                });
        });

        it('should notify BAD_READ_RESPONSE for GET reject', (done) => {
            global.fetch.mockImplementation(() => Promise.reject('rejected!'));
            fetchCloudApi('some/path')(dispatch, defaultGetState).catch((error) => {
                expect(error).toEqual('rejected!');
                expect(dispatch).toBeCalled();
                expect(notify).toBeCalled();
                expect(popFnCalls(notify)[0]).toEqual([{ text: ERRORS.BAD_READ_RESPONSE }]);
                done();
            });
        });

        it('should notify BAD_WRITE_RESPONSE for POST reject', (done) => {
            global.fetch.mockImplementation(() => Promise.reject('rejected!'));
            fetchCloudApi('some/path', { method: 'POST' })(dispatch, defaultGetState).catch((error) => {
                expect(error).toEqual('rejected!');
                expect(dispatch).toBeCalled();
                expect(notify).toBeCalled();
                expect(popFnCalls(notify)[0]).toEqual([{ text: ERRORS.BAD_WRITE_RESPONSE }]);
                done();
            });
        });

        it('should notify BAD_WRITE_RESPONSE for DELETE reject', (done) => {
            global.fetch.mockImplementation(() => Promise.reject('rejected!'));
            fetchCloudApi('some/path', { method: 'DELETE' })(dispatch, defaultGetState).catch((error) => {
                expect(error).toEqual('rejected!');
                expect(dispatch).toBeCalled();
                expect(notify).toBeCalled();
                expect(popFnCalls(notify)[0]).toEqual([{ text: ERRORS.BAD_WRITE_RESPONSE }]);
                done();
            });
        });

        it('should notify BAD_WRITE_RESPONSE for PUT reject', (done) => {
            global.fetch.mockImplementation(() => Promise.reject('rejected!'));
            fetchCloudApi('some/path', { method: 'PUT' })(dispatch, defaultGetState).catch((error) => {
                expect(error).toEqual('rejected!');
                expect(dispatch).toBeCalled();
                expect(notify).toBeCalled();
                expect(popFnCalls(notify)[0]).toEqual([{ text: ERRORS.BAD_WRITE_RESPONSE }]);
                done();
            });
        });

        it('should not notify for 409', (done) => {
            global.fetch.mockImplementation(() => Promise.reject({ status: 409 }));
            fetchCloudApi('some/path', { method: 'PUT' })(dispatch, defaultGetState).catch((error) => {
                expect(error).toEqual({ status: 409 });
                expect(dispatch).not.toBeCalled();
                expect(notify).not.toBeCalled();
                done();
            });
        });
    });

    describe('checkNoteExist =>', () => {
        const state = {
            notes: {
                notes: {
                    first: {
                        id: 'first',
                        tags: {}
                    },
                    second: {
                        id: 'second',
                        tags: {
                            deleted: true
                        }
                    }
                }
            }
        };
        it('should return true if note exists', () => {
            expect(checkNoteExist(state, 'first')).toEqual(true);
        });
        it('should return false if note exists but it was deleted', () => {
            expect(checkNoteExist(state, 'second')).toEqual(false);
        });
        it('should return false if note does not exist', () => {
            expect(checkNoteExist(state, 'third')).toEqual(false);
        });
    });

    describe('updateNote =>', () => {
        const getState = () => ({
            notes: {
                notes: {
                    first: {
                        id: 'first',
                        tags: {}
                    }
                }
            }
        });
        it('should reject if note does not exist', (done) => {
            updateNote({ id: 'not-existing' })(dispatch, getState).catch(() => {
                expect(dispatch).not.toBeCalled();
                done();
            });
        });
        it('should call UPDATE_NOTE action', () => {
            const note = { id: 'first', title: '1st' };
            updateNote(note)(dispatch, getState);
            expect(dispatch).toBeCalled();
            expect(popFnCalls(dispatch)[0]).toEqual([{
                type: UPDATE_NOTE,
                payload: {
                    note
                }
            }]);
        });
    });

    describe('addNote', () => {
        it('should call ADD_NOTE action', () => {
            const note = {
                id: 'note-id',
                ctime: 12345,
                mtime: 67890,
                title: 'title',
                snippet: 'snippet',
                content: {
                    state: STATES.LOADED,
                    data: {
                        name: '$root',
                        children: [{
                            name: 'paragraph'
                        }]
                    }
                },
                tags: {
                    pin: true
                },
                attachments: {
                    'first-attach': {
                        resourceId: 'first-attach',
                        preview: 'first-preview-url',
                        file: 'first-file-url'
                    },
                    'second-attach': {
                        resourceId: 'second-attach',
                        preview: 'second-preview-url',
                        file: 'second-file-url'
                    }
                },
                attachmentOrder: ['first-attach', 'second-attach']
            };
            const action = addNote(note);
            expect(action).toEqual({
                type: ADD_NOTE,
                payload: {
                    note
                }
            });
        });
        it('should call ADD_NOTE action with default parameters', () => {
            const action = addNote({ id: 'note-id' });
            expect(action).toEqual({
                type: ADD_NOTE,
                payload: {
                    note: {
                        id: 'note-id',
                        ctime: undefined,
                        mtime: undefined,
                        title: undefined,
                        snippet: '',
                        content: {
                            state: STATES.INITIAL,
                            data: null
                        },
                        tags: {},
                        attachments: {},
                        attachmentOrder: []
                    }
                }
            });
        });
    });
});
