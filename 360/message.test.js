'use strict';

const { NO_IDS, NO_SUCH_MESSAGE } = require('../../lib/error');
const model = require('./message');

let core;

beforeEach(function() {
    core = {
        request: jest.fn(),
        params: {
            models: [
                {
                    name: 'message',
                    params: {
                        ids: '12345'
                    }
                }
            ]
        },
        yasm: {
            sum: jest.fn()
        }
    };
});

test('throws NO_IDS', async () => {
    expect.assertions(1);
    try {
        await model({}, core);
    } catch (e) {
        expect(e).toBeInstanceOf(NO_IDS);
    }
});

test('plaint message request', async () => {
    core.request.mockResolvedValueOnce({ message: [ { mid: '12345' } ] });

    const res = await model({ onlyPlainMsg: true, ids: '12345' }, core);

    expect(res).toEqual({ mid: '12345' });
});

test('plaint message request with retry', async () => {
    core.request
        .mockResolvedValueOnce({ message: [] })
        .mockResolvedValueOnce({ message: [ { mid: '12345' } ] });

    const res = await model({ onlyPlainMsg: true, ids: '12345' }, core);

    expect(res).toEqual({ mid: '12345' });
});

test('calls request with dbtype=master', async () => {
    core.request.mockResolvedValueOnce({ message: [ { mid: '12345' } ] });

    await model({ force: 'true', ids: '12345' }, core);

    expect(core.request).toHaveBeenCalledTimes(1);
    expect(core.request).toHaveBeenCalledWith('messages', expect.objectContaining({
        dbtype: 'master'
    }));
});

test('calls request without dbtype=master', async () => {
    core.request.mockResolvedValueOnce({ message: [ { mid: '12345' } ] });

    await model({ ids: '12345' }, core);

    expect(core.request).toHaveBeenCalledTimes(1);
    expect(core.request).toHaveBeenCalledWith('messages', expect.not.objectContaining({
        dbtype: 'master'
    }));
});

test('retries with dbtype=mater', async () => {
    core.request
        .mockResolvedValueOnce({ message: [] })
        .mockResolvedValueOnce({ message: [ { mid: '12345' } ] });

    await model({ ids: '12345' }, core);

    expect(core.request).toHaveBeenCalledTimes(2);
    expect(core.request).toHaveBeenCalledWith('messages', expect.objectContaining({
        dbtype: 'master'
    }));
    expect(core.request).toHaveBeenCalledWith('messages', expect.not.objectContaining({
        dbtype: 'master'
    }));
    expect(core.yasm.sum).toHaveBeenCalledTimes(1);
    expect(core.yasm.sum).toHaveBeenCalledWith('resolveStidRetry');
});

test('retries, but message still not found', async () => {
    expect.assertions(5);
    core.request
        .mockResolvedValueOnce({ message: [] })
        .mockResolvedValueOnce({ message: [] });

    try {
        await model({ ids: '12345' }, core);
    } catch (e) {
        expect(e).toBeInstanceOf(NO_SUCH_MESSAGE);
    }

    expect(core.request).toHaveBeenCalledTimes(2);
    expect(core.yasm.sum).toHaveBeenCalledTimes(2);
    expect(core.yasm.sum).toHaveBeenCalledWith('resolveStidRetry');
    expect(core.yasm.sum).toHaveBeenCalledWith('noSuchMessage_noStid');
});
