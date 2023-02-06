'use strict';

const changes = require('./changes.js');
const ApiError = require('../../../routes/helpers/api-error.js');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const { AKITA_ERROR } = require('@ps-int/mail-lib').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;
let successResponse;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            foo: 'bar'
        },
        service: () => mockService
    };
    successResponse = { baz: 'qwe' };
});

test('ходит в meta и прокидывает параметры', async () => {
    mockService.mockResolvedValueOnce(successResponse);

    await changes(core);

    expect(mockService).toHaveBeenCalledWith('/v2/changes', { foo: 'bar' });
    const xivaParams = mockService.mock.calls[0][1];
    expect(xivaParams.foo).toBe('bar');
});

test('определяет расстояние между ревизиями, получает их и отдает запрошенное кол-во', async () => {
    mockService
        .mockResolvedValueOnce({ mailbox_revision: 1010 })
        .mockResolvedValueOnce({ changes: [ '1', '2', '3', '4', '5', '6', '7', '8', '9', '10' ] });
    core.params = {
        new: true,
        max_count: 3,
        revision: 1000
    };

    const res = await changes(core);

    expect(mockService).toHaveBeenCalledWith('/mailbox_revision');
    expect(mockService).toHaveBeenCalledWith('/v2/changes', {
        max_count: 11,
        revision: 1000
    });
    const xivaParams = mockService.mock.calls[1][1];
    expect(xivaParams.max_count).toBe(11);
    expect(xivaParams.revision).toBe(1000);
    expect(xivaParams.new).toBeUndefined();
    expect(res.changes).toHaveLength(3);
    expect(res.changes).toEqual([ '1', '2', '3' ]);
});

test('заменяет настоящий fid на фиктивный при withTabs=1 для типов store, quickSave', async () => {
    mockService
        .mockResolvedValueOnce({ mailbox_revision: 1010 })
        .mockResolvedValueOnce({
            changes: [
                {
                    type: 'store',
                    value: [ { fid: '1', tab: 'relevant' } ]
                },
                {
                    type: 'quickSave',
                    value: [ { fid: '2', tab: 'social' } ]
                },
                {
                    type: 'move',
                    value: [ { fid: '3', tab: 'news' } ]
                }
            ]
        });
    core.params = {
        new: true,
        max_count: 3,
        revision: 1000,
        withTabs: true
    };

    const res = await changes(core);

    expect(mockService).toHaveBeenCalledWith('/mailbox_revision');
    expect(mockService).toHaveBeenCalledWith('/v2/changes', {
        max_count: 11,
        revision: 1000
    });
    const xivaParams = mockService.mock.calls[1][1];
    expect(xivaParams.max_count).toBe(11);
    expect(xivaParams.revision).toBe(1000);
    expect(xivaParams.new).toBeUndefined();
    expect(xivaParams.withTabs).toBeUndefined();
    expect(res.changes).toHaveLength(3);
    expect(res.changes).toEqual([
        {
            type: 'store',
            value: [ { fid: '-10', tab: 'relevant' } ]
        },
        {
            type: 'quickSave',
            value: [ { fid: '-12', tab: 'social' } ]
        },
        {
            type: 'move',   // Mind the type
            value: [ { fid: '3', tab: 'news' } ]
        }
    ]);
});

test('добавляет событие folder-counters-update, если были события update (без табов)', async () => {
    mockService
        .mockResolvedValueOnce({ mailbox_revision: 1010 })
        .mockResolvedValueOnce({
            changes: [
                {
                    revision: 1,
                    type: 'update',
                    value: [ { mid: '1', labels: [] } ]
                },
                {
                    revision: 2,
                    type: 'update',
                    value: [ { mid: '1', labels: [] } ]
                },
                {
                    revision: 3,
                    type: 'update',
                    value: [ { mid: '2', labels: [] } ]
                }
            ]
        })
        .mockResolvedValueOnce({
            folders: {
                1: {
                    newMessagesCount: 10,
                    messagesCount: 100
                },
                2: {
                    newMessagesCount: 20,
                    messagesCount: 200
                }
            }
        })
        .mockResolvedValueOnce({
            envelopes: [
                {
                    mid: '1',
                    fid: '1',
                    tab: ''
                },
                {
                    mid: '2',
                    fid: '2',
                    tab: ''
                }
            ]
        });
    core.params = {
        new: true,
        max_count: 3,
        revision: 1000
    };

    const res = await changes(core);

    expect(mockService).toHaveBeenCalledWith('/mailbox_revision');
    expect(mockService).toHaveBeenCalledWith('/folders', {});
    expect(mockService).toHaveBeenCalledWith('/filter_search', { mids: [ '1', '2' ] });
    expect(mockService).not.toHaveBeenCalledWith('v2/tabs');
    const xivaParams = mockService.mock.calls[1][1];
    expect(xivaParams.max_count).toBe(11);
    expect(xivaParams.revision).toBe(1000);
    expect(xivaParams.new).toBeUndefined();
    expect(xivaParams.withTabs).toBeUndefined();
    expect(res.changes).toHaveLength(4);
    expect(res.changes).toEqual([
        {
            revision: 1,
            type: 'update',
            value: [ { mid: '1', labels: [] } ]
        },
        {
            revision: 2,
            type: 'update',
            value: [ { mid: '1', labels: [] } ]
        },
        {
            revision: 3,
            type: 'update',
            value: [ { mid: '2', labels: [] } ]
        },
        {
            revision: 3,
            type: 'folder-counters-update',
            value: [
                {
                    fid: '1',
                    unread: 10,
                    total: 100
                },
                {
                    fid: '2',
                    unread: 20,
                    total: 200
                }
            ]
        }
    ]);
});

test('добавляет событие folder-counters-update, если были события update (с табами)', async () => {
    mockService
        .mockResolvedValueOnce({ mailbox_revision: 1010 })
        .mockResolvedValueOnce({
            changes: [
                {
                    revision: 1,
                    type: 'update',
                    value: [ { mid: '1', labels: [] } ]
                },
                {
                    revision: 2,
                    type: 'update',
                    value: [ { mid: '1', labels: [] } ]
                },
                {
                    revision: 3,
                    type: 'update',
                    value: [ { mid: '2', labels: [] } ]
                },
                {
                    revision: 4,
                    type: 'update',
                    value: [ { mid: '3', labels: [] } ]
                }
            ]
        })
        .mockResolvedValueOnce({
            folders: {
                1: {
                    newMessagesCount: 10,
                    messagesCount: 100
                },
                2: {
                    newMessagesCount: 20,
                    messagesCount: 200
                },
                3: {
                    newMessagesCount: 30,
                    messagesCount: 300
                }
            }
        })
        .mockResolvedValueOnce({
            envelopes: [
                {
                    mid: '1',
                    fid: '1',
                    tab: 'news'
                },
                {
                    mid: '2',
                    fid: '2',
                    tab: 'relevant'
                },
                {
                    mid: '3',
                    fid: '3',
                    tab: ''
                }
            ]
        })
        .mockResolvedValueOnce({
            tabs: [
                {
                    type: 'relevant',
                    unreadMessagesCount: 15,
                    messagesCount: 150
                },
                {
                    type: 'news',
                    unreadMessagesCount: 25,
                    messagesCount: 250
                }
            ]
        });
    core.params = {
        new: true,
        max_count: 4,
        revision: 1000,
        withTabs: true
    };

    const res = await changes(core);

    expect(mockService.mock.calls[0]).toEqual([ '/mailbox_revision' ]);
    expect(mockService.mock.calls[1][0]).toBe('/v2/changes');
    const xivaParams = mockService.mock.calls[1][1];
    expect(xivaParams.max_count).toBe(11);
    expect(xivaParams.revision).toBe(1000);
    expect(xivaParams.new).toBeUndefined();
    expect(xivaParams.withTabs).toBeUndefined();
    expect(mockService.mock.calls[2]).toEqual([ '/folders', {} ]);
    expect(mockService.mock.calls[3]).toEqual([ '/filter_search', { mids: [ '1', '2', '3' ] } ]);
    expect(mockService.mock.calls[4]).toEqual([ '/v2/tabs', {} ]);
    expect(res.changes).toHaveLength(5);
    expect(res.changes).toEqual([
        {
            revision: 1,
            type: 'update',
            value: [ { mid: '1', labels: [] } ]
        },
        {
            revision: 2,
            type: 'update',
            value: [ { mid: '1', labels: [] } ]
        },
        {
            revision: 3,
            type: 'update',
            value: [ { mid: '2', labels: [] } ]
        },
        {
            revision: 4,
            type: 'update',
            value: [ { mid: '3', labels: [] } ]
        },
        {
            revision: 4,
            type: 'folder-counters-update',
            value: [
                {
                    fid: '-11',
                    tab: 'news',
                    unread: 25,
                    total: 250
                },
                {
                    fid: '-10',
                    tab: 'relevant',
                    unread: 15,
                    total: 150
                },
                {
                    fid: '3',
                    tab: '',
                    unread: 30,
                    total: 300
                }
            ]
        }
    ]);
});

test('проксирует ответ', async () => {
    mockService.mockResolvedValueOnce(successResponse);

    const response = await changes(core);

    expect(response).toEqual({
        baz: 'qwe'
    });
});

test('если сервис отвечает 400 + 5001 (invalid arguments) отвечаем 400', async () => {
    expect.assertions(2);
    mockService.mockRejectedValueOnce(new AKITA_ERROR({
        error: {
            error: {
                code: 5001,
                message: 'invalid argument',
                reason: 'invalid max_count argument'
            }
        }
    }));

    try {
        await changes(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
    }
});

test('если сервис отвечает 400 + 5013 (the revision can not be found) отвечаем 400', async () => {
    expect.assertions(2);
    mockService.mockRejectedValueOnce(new AKITA_ERROR({
        error: {
            error: {
                code: 5013,
                message: 'the revision can not be found',
                reason: 'revision has not been found in changelog'
            }
        }
    }));

    try {
        await changes(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
    }
});

test('если сервис валится, отвечаем 500', async () => {
    expect.assertions(2);
    mockService.mockRejectedValueOnce(httpError(500));

    try {
        await changes(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
    }
});
