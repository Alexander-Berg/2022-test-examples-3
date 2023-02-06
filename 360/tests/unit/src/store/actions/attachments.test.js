import 'isomorphic-fetch'; // new Response() fails without it
import {
    batchRequestAttachments,
    batchFetchCloudApi
} from '../../../../../src/store/actions/attachments';

import { UPDATE_ATTACHMENT } from '../../../../../src/store/types';
import { STATES, HTTP_STATUSES } from '../../../../../src/consts';

jest.useFakeTimers();

jest.mock('../../../../../src/store/actions/common', () => ({
    checkNoteExist: jest.fn(() => true),
    getCloudApiBaseUrl: jest.fn(() => 'https://cloud-api.yandex.net/yadisk_web/')
}));
jest.mock('../../../../../src/store/actions/', () => ({
    unauthAndReloadPage: jest.fn()
}));
import { getCloudApiBaseUrl } from '../../../../../src/store/actions/common';
import { unauthAndReloadPage } from '../../../../../src/store/actions/';

describe('store/actions/attachments', () => {
    global.fetch = jest.fn();
    const userUid = '001';
    const defaultGetState = (current = null) => ({
        user: { id: userUid },
        services: {
            'cloud-api': 'https://cloud-api.yandex.net'
        },
        environment: {
            fullTld: 'net'
        },
        notes: {
            current,
            notes: {
                'note-1': {
                    id: 'note-1',
                    attachments: {
                        'note-1-attach-1': {
                            resourceId: 'note-1-attach-1',
                            state: STATES.INITIAL
                        },
                        'note-1-attach-2': {
                            resourceId: 'note-1-attach-2',
                            state: STATES.INITIAL
                        },
                        'note-1-attach-3': {
                            resourceId: 'note-1-attach-3',
                            state: STATES.INITIAL
                        },
                        'note-1-attach-4': {
                            resourceId: 'note-1-attach-4',
                            state: STATES.INITIAL
                        }
                    },
                    attachmentOrder: [
                        'note-1-attach-1',
                        'note-1-attach-2',
                        'note-1-attach-3',
                        'note-1-attach-4'
                    ]
                },
                'note-2': {
                    id: 'note-2',
                    attachments: {
                        'note-2-attach-1': {
                            resourceId: 'note-2-attach-1',
                            state: STATES.INITIAL
                        },
                        'note-2-attach-2': {
                            resourceId: 'note-2-attach-2',
                            state: STATES.INITIAL
                        },
                        'note-2-attach-3': {
                            resourceId: 'note-2-attach-3',
                            state: STATES.INITIAL
                        }
                    },
                    attachmentOrder: [
                        'note-2-attach-1',
                        'note-2-attach-2',
                        'note-2-attach-3'
                    ]
                },
                'note-3': {
                    id: 'note-3',
                    attachments: {},
                    attachmentOrder: []
                }
            }
        }
    });

    const commonFetchOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8', 'X-Uid': userUid },
        credentials: 'include'
    };

    const dispatch = jest.fn(
        (arg, currentNote) => typeof arg === 'function' ?
            arg(dispatch, () => defaultGetState(currentNote)) :
            undefined
    );

    /**
     * @typedef MockedResponse
     * @param {string} body
     * @param {number} code
     */

    /**
     * @param {Array<Array>} noteAttachmentPairs - pairs `[i, j]`, where `i` is `1` in `note-1`
     * and `j` is `2` in `note-1-attach-2`
     * @returns {MockedResponse[]} array of mocked responses
     */
    const getMockedFetchResponseItems = (noteAttachmentPairs) => noteAttachmentPairs.map(([noteN, attachmentN]) => ({
        body: `{"resource_id":"note-${noteN}-attach-${attachmentN}","mime_type":"image/png","file":"/path/to/file","media_type":"image","preview":"/path/to/preview"}`,
        code: 200
    }));

    afterAll(() => {
        jest.resetAllMocks();
    });

    describe('batchFetchCloudApi', () => {
        const getMockItems = () => [1, 2].map((i) => ({
            method: 'GET',
            relative_url: `/note-${i}-attach-1`,
            noteId: `note-${i}`
        }));

        afterEach(() => {
            dispatch.mockClear();
        });

        it('should call the callback function with items returned with 200 status', async() => {
            const onSuccess = jest.fn();
            const response = getMockedFetchResponseItems([[1, 1], [2, 1]]);
            fetch.mockResolvedValueOnce(new Response(JSON.stringify({ items: response })));

            const items = getMockItems();
            await dispatch(batchFetchCloudApi({ items, onSuccess }));

            expect(fetch).toHaveBeenCalledWith(`${getCloudApiBaseUrl()}v1/batch/request`, {
                ...commonFetchOptions,
                body: JSON.stringify({ items })
            });

            expect(dispatch).toHaveBeenCalledTimes(1);
            expect(onSuccess).toHaveBeenCalledWith(response.map((item, i) => ({ ...item, noteId: `note-${++i}` })));
        });

        it('should retry for items with a status code different from 200', async() => {
            const onSuccess = jest.fn();
            const response = getMockedFetchResponseItems([[1, 1], [2, 1]]);
            response[1].code = 500;
            fetch.mockResolvedValueOnce(new Response(JSON.stringify({ items: response })));
            fetch.mockResolvedValueOnce(new Response(JSON.stringify({ items: response.slice(1).map((item) => ({ ...item, code: 200 })) })));

            const items = getMockItems();
            await dispatch(batchFetchCloudApi({ items, onSuccess }));

            expect(dispatch).toHaveBeenCalledTimes(2);
            expect(onSuccess).toHaveBeenCalledWith(response
                .filter((item) => item.code === 200)
                .map((item, i) => ({ ...item, noteId: `note-${++i}` }))
            );
        });

        it('should call onError callback if `attempts` is 0 and fetch fails', async() => {
            const onError = jest.fn();
            fetch.mockRejectedValueOnce();

            await dispatch(batchFetchCloudApi({ items: [], onError }, 0));

            expect(onError).toHaveBeenCalledTimes(1);
        });

        it('should call unauthAndReloadPage and reload page if response code is 401 (UNAUTHORIZED)', async() => {
            const response = { status: HTTP_STATUSES.UNAUTHORIZED };

            fetch.mockRejectedValueOnce(response);
            await dispatch(batchFetchCloudApi({ items: [] }));
            expect(unauthAndReloadPage).toBeCalled();
        });
    });

    describe('batchRequestAttachments', () => {
        afterEach(() => {
            dispatch.mockClear();
        });

        const testCallsToUpdateAttachment = (calls = []) => {
            // get all the calls of the `dispatch` that update the store
            const storeUpdateCalls = calls.filter(([args]) => typeof args === 'object');

            // expect every call to be of type `UPDATE_ATTACHMENT`
            storeUpdateCalls.forEach(([args], i) => {
                expect(args.type).toEqual(UPDATE_ATTACHMENT);
                // first half of the calls should set the state of the attachment to `LOADING`
                // the other half puts the received META data in the store and marks the state as `LOADED`
                if (i < storeUpdateCalls.length / 2) {
                    expect(args.payload.data.state).toEqual(STATES.LOADING);
                } else {
                    expect(args.payload.resourceId.startsWith(args.payload.noteId)).toBe(true);
                    expect(args.payload.data.preview).not.toBe(undefined);
                    expect(args.payload.data.state).toEqual(STATES.LOADED);
                }
            });
        };

        it('should request attachments META info for every first attachment of each note', async() => {
            const fetchResponseItems = getMockedFetchResponseItems([[1, 1], [2, 1]]);

            fetch.mockResolvedValueOnce(new Response(JSON.stringify({ items: fetchResponseItems })));
            await dispatch(batchRequestAttachments());

            const fetchRequestParams = {
                ...commonFetchOptions,
                body: JSON.stringify({
                    items: [1, 2].map((i) => ({
                        method: 'GET',
                        relative_url: `/yadisk_web/v1/disk/notes/note-${i}-attach-1`,
                        noteId: `note-${i}`,
                        resourceId: `note-${i}-attach-1`
                    }))
                })
            };

            expect(fetch).toHaveBeenCalledWith(`${getCloudApiBaseUrl()}v1/batch/request`, fetchRequestParams);

            testCallsToUpdateAttachment(dispatch.mock.calls);
        });

        [1, 2].forEach((noteN) => {
            it('should request attachments META for all the attachments of a given note', async() => {
                const noteAttachmentPairs = defaultGetState().notes.notes[`note-${noteN}`].attachmentOrder.map((_, i) => ([noteN, ++i]));
                const fetchResponseItems = getMockedFetchResponseItems(noteAttachmentPairs);

                fetch.mockResolvedValueOnce(new Response(JSON.stringify({ items: fetchResponseItems })));

                await dispatch(batchRequestAttachments(`note-${noteN}`));

                const fetchRequestParams = {
                    ...commonFetchOptions,
                    body: JSON.stringify({
                        items: noteAttachmentPairs.map(([, i]) => ({
                            method: 'GET',
                            relative_url: `/yadisk_web/v1/disk/notes/note-${noteN}-attach-${i}`,
                            noteId: `note-${noteN}`,
                            resourceId: `note-${noteN}-attach-${i}`
                        }))
                    })
                };

                expect(fetch).toHaveBeenCalledWith(`${getCloudApiBaseUrl()}v1/batch/request`, fetchRequestParams);

                testCallsToUpdateAttachment(dispatch.mock.calls);
            });
        });

        it('should request every first attachment plus all attachments of the current note', async() => {
            const noteIdAttachmentIdPairs = [[2, 1], [1, 1], [1, 2], [1, 3], [1, 4]];
            const fetchResponseItems = getMockedFetchResponseItems(noteIdAttachmentIdPairs);

            fetch.mockResolvedValueOnce(new Response(JSON.stringify({ items: fetchResponseItems })));
            await dispatch(batchRequestAttachments(), 'note-1');

            const fetchRequestParams = {
                ...commonFetchOptions,
                body: JSON.stringify({
                    items: noteIdAttachmentIdPairs.map(([noteId, attachmentId]) => ({
                        method: 'GET',
                        relative_url: `/yadisk_web/v1/disk/notes/note-${noteId}-attach-${attachmentId}`,
                        noteId: `note-${noteId}`,
                        resourceId: `note-${noteId}-attach-${attachmentId}`
                    }))
                })
            };

            expect(fetch).toHaveBeenCalledWith(`${getCloudApiBaseUrl()}v1/batch/request`, fetchRequestParams);

            testCallsToUpdateAttachment(dispatch.mock.calls);
        });
    });
});
