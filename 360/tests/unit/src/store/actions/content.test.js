import { updateNoteContent } from '../../../../../src/store/actions/content';
import { MAX_NOTE_CONTENT_LENGTH } from '../../../../../src/consts';
import { ADD_NOTE, UPDATE_NOTE } from '../../../../../src/store/types';

jest.mock('../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn(),
    countError: jest.fn()
}));
import { countError } from '../../../../../src/helpers/metrika';

const getUpdateNoteDispatches = (dispatchCalls) => dispatchCalls.filter((callItem) => typeof callItem[0] === 'object' && callItem[0].type === UPDATE_NOTE);

describe('store/actions/content =>', () => {
    const testNoteId = 'test-note';
    const duplicateNoteId = 'duplicate-note';
    const cloudApiOrigin = 'https://cloud-api.yandex.ru';
    const defaultGetState = () => ({
        notes: {
            notes: {
                [testNoteId]: {
                    id: testNoteId,
                    ctime: '2019-02-09T22:42:13.114Z',
                    mtime: '2019-05-15T14:51:05.129Z',
                    title: 'нотатка',
                    snippet: 'content',
                    content: {
                        data: {
                            name: '$root',
                            children: [{
                                name: 'paragraph',
                                children: [{
                                    data: 'content'
                                }]
                            }]
                        },
                        revision: 122,
                        length: 7
                    },
                    tags: {
                        pin: true
                    },
                    attachments: {
                        'first-attach-id': {},
                        'second-attach-id': {}
                    },
                    attachmentOrder: ['first-attach-id', 'second-attach-id']
                }
            }
        },
        services: {
            'cloud-api': cloudApiOrigin
        },
        notifications: {},
        user: { id: '001' }
    });
    const dispatch = jest.fn(
        (arg) => typeof arg === 'function' ?
            arg(dispatch, defaultGetState) :
            undefined
    );

    const newSnippet = 'new content';
    const newContent = {
        name: '$root',
        children: [{
            name: 'paragraph',
            children: [{
                data: 'new content'
            }]
        }]
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('updateNoteContent', () => {
        const originalFetch = global.fetch;
        const OriginalDate = global.Date;
        beforeAll(() => {
            global.fetch = jest.fn();
            global.Date = function() {
                // mock `new Date()` because date format in list depends on time from note creation till now
                return new OriginalDate(1557840899167);
            };
            global.Date.toISOString = OriginalDate.toISOString;
        });
        afterAll(() => {
            global.fetch = originalFetch;
            global.Date = OriginalDate;
        });

        it('should call `content_with_meta` API method and update note', async() => {
            global.fetch.mockResolvedValue({
                ok: true,
                status: 200,
                headers: {
                    get: (header) => header === 'X-Actual-Revision' ? 123 : undefined
                },
                json: () => Promise.resolve({})
            });

            await updateNoteContent(testNoteId, {
                content: newContent,
                snippet: newSnippet,
                length: 11
            })(dispatch);

            expect(dispatch).toBeCalled();
            const dispatchCalls = popFnCalls(dispatch);
            const updateNoteCalls = getUpdateNoteDispatches(dispatchCalls);
            // первый апдейт выставляет `saving: true` перед вызовом ручки, второй обновляет контент после ответа ручки
            expect(updateNoteCalls.length).toEqual(2);
            expect(updateNoteCalls).toMatchSnapshot();

            expect(global.fetch).toBeCalled();
            expect(popFnCalls(global.fetch)).toMatchSnapshot();
        });

        it('should not update note and call API if note does not exist', async() => {
            await updateNoteContent('not-existing-note', {})(dispatch);

            const dispatchCalls = popFnCalls(dispatch);
            expect(getUpdateNoteDispatches(dispatchCalls).length).toEqual(0);
            expect(global.fetch).not.toBeCalled();
        });

        it('should update note and count error if note length exceed maximum', async() => {
            await updateNoteContent(testNoteId, {
                length: MAX_NOTE_CONTENT_LENGTH + 1
            })(dispatch);

            const dispatchCalls = popFnCalls(dispatch);
            const updateNoteCalls = getUpdateNoteDispatches(dispatchCalls);
            // апдейт длины контента в заметке (нужен для показа нотифайки про первышение лимита)
            expect(updateNoteCalls.length).toEqual(1);
            expect(updateNoteCalls).toMatchSnapshot();

            expect(global.fetch).not.toBeCalled();
            expect(countError).toBeCalled();
            expect(popFnCalls(countError)).toEqual([[`more than ${MAX_NOTE_CONTENT_LENGTH} symbols in editor`]]);
        });

        it('should set error to note on API request fail', async() => {
            global.fetch.mockRejectedValue('!!error!!');
            await updateNoteContent(testNoteId, {
                snippet: '',
                content: {},
                length: 0
            })(dispatch);

            expect(global.fetch).toBeCalled();
            const dispatchCalls = popFnCalls(dispatch);
            const updateNoteCalls = getUpdateNoteDispatches(dispatchCalls);
            // первый апдейт выставляет `saving: true` перед вызовом ручки, второй выставляет `saving: false, error: *` после завершения ручки ошибкой
            expect(updateNoteCalls.length).toEqual(2);
            expect(updateNoteCalls).toMatchSnapshot();
        });

        it('should create duplicate on conflict', async() => {
            global.fetch.mockImplementation((url, { body }) => {
                if (/content_with_meta/.test(url)) {
                    return Promise.resolve({
                        status: 409
                    });
                }
                if (/clone_with_new_content/.test(url)) {
                    const parsedBody = JSON.parse(body);
                    return Promise.resolve({
                        ok: true,
                        status: 200,
                        headers: {
                            get: (header) => header === 'X-Actual-Revision' ? 123 : undefined
                        },
                        json: () => Promise.resolve({
                            attach_resource_ids: ['first-attach-copy-id', 'second-attach-copy-id'],
                            content_revision: 0,
                            ctime: '2019-05-15T14:51:06.180Z',
                            id: duplicateNoteId,
                            mtime: '2019-05-15T14:51:06.180Z',
                            snippet: parsedBody.snippet,
                            tags: [2],
                            tags_with_meta: [{
                                id: 2,
                                mtime: '2019-05-15T14:50:53.981Z'
                            }],
                            title: parsedBody.title
                        })
                    });
                }
            });

            await updateNoteContent(testNoteId, {
                snippet: newSnippet,
                content: newContent,
                length: 11
            })(dispatch);

            const fetchCalls = popFnCalls(global.fetch);
            // первый fetch - на обновление контента, вернувший 409, второй - на создание дубликата
            expect(fetchCalls.length).toEqual(2);
            expect(fetchCalls).toMatchSnapshot();

            const dispatchCalls = popFnCalls(dispatch);
            const updateNoteCalls = getUpdateNoteDispatches(dispatchCalls);
            // должны быть такие апдейты:
            // 1) `saving: true` для исходной заметки
            // 2) `conflict: true` для исходной заметки после того как ручка ответила 409
            // 3) `saving: false, conflict: false` для исходной заметки после создания дубля
            expect(updateNoteCalls.length).toEqual(3);
            expect(updateNoteCalls).toMatchSnapshot();

            const addNoteCalls = dispatchCalls.filter((callItem) => typeof callItem[0] === 'object' && callItem[0].type === ADD_NOTE);
            // добавление заметки-дубля
            expect(addNoteCalls.length).toEqual(1);
            expect(addNoteCalls[0][0].payload).toMatchSnapshot();
        });

        it('should not call API for note in conflicted state', async() => {
            const getState = () => ({
                notes: {
                    notes: {
                        [testNoteId]: {
                            id: testNoteId,
                            conflict: true
                        }
                    }
                }
            });
            const dispatch = jest.fn(
                (arg) => typeof arg === 'function' ?
                    arg(dispatch, getState) :
                    undefined
            );

            await updateNoteContent(testNoteId, {})(dispatch);
            expect(global.fetch).not.toBeCalled();
            expect(getUpdateNoteDispatches(popFnCalls(dispatch)).length).toEqual(0);
        });

        it('should push update to queue if note content already updating', async() => {
            const fetchPromises = [];
            global.fetch.mockImplementation(() => {
                const promise = new Promise((resolve) => {
                    setTimeout(() => resolve({
                        ok: true,
                        status: 200,
                        headers: {
                            get: (header) => header === 'X-Actual-Revision' ? 123 : undefined
                        },
                        json: () => Promise.resolve({})
                    }), 30);
                });
                fetchPromises.push(promise);
                return promise;
            });

            updateNoteContent(testNoteId, {
                snippet: newSnippet,
                content: newContent,
                length: 11
            })(dispatch);

            // вызвали первый апдейт
            expect(global.fetch).toBeCalledTimes(1);
            expect(getUpdateNoteDispatches(dispatch.mock.calls).length).toEqual(1);

            const newContentClone = JSON.parse(JSON.stringify(newContent));
            newContentClone.children[0].children[0].data += '123';
            updateNoteContent(testNoteId, {
                snippet: newSnippet + '123',
                content: newContentClone,
                length: 14
            })(dispatch);

            await fetchPromises[0];
            // второй апдейт не вызвали (так как первый ещё в процессе)
            expect(global.fetch).toBeCalledTimes(1);
            expect(getUpdateNoteDispatches(dispatch.mock.calls).length).toEqual(1);

            // после завершения запроса нужно дать выполниться всем колбэкам - для этого ждём 1 tick
            await new Promise((resolve) => {
                setTimeout(() => resolve());
            });

            // запустилось второе обновление
            expect(global.fetch).toBeCalledTimes(2);
            expect(getUpdateNoteDispatches(dispatch.mock.calls).length).toEqual(3);

            await fetchPromises[1];

            // после завершения запроса нужно дать выполниться всем колбэкам - для этого ждём 1 tick
            await new Promise((resolve) => {
                setTimeout(() => resolve());
            });

            expect(global.fetch).toBeCalledTimes(2);
            const finalUpdates = getUpdateNoteDispatches(popFnCalls(dispatch));
            // 2 обновления запустили по 2 апдейта (`saving: true` + обновление контента)
            expect(finalUpdates.length).toEqual(4);

            expect(popFnCalls(global.fetch)).toMatchSnapshot();
            expect(finalUpdates).toMatchSnapshot();
        });
    });
});
