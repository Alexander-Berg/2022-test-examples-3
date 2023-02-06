'use strict';

const throttler = require('./throttler.js');
const _times = require('lodash/times');

const requestsFactory = (count) => _times(count).map((i) => i);

const OPTIONS = {
    delay: 10,
    portion: 10,
    maxExecutionTime: 1500
};

test('если не передать requests, возвращает []', async () => {
    const res = await throttler();

    expect(res).toEqual([]);
});

test('отрабатывает все запросы', async () => {
    const res = await throttler(requestsFactory(100), OPTIONS);

    expect(res).toHaveLength(100);
});

test('возвращает что успел, если не уложился в лимит', async () => {
    const res = await throttler(requestsFactory(100), { portion: 10, delay: 20, maxExecutionTime: 10 });

    expect(res).toHaveLength(10);
});

describe('принимает в качестве delay функцию', () => {
    it('вызывает ее с аргументом n = текущему запросу', async () => {
        const delayStub = jest.fn().mockReturnValue(100);

        await throttler(requestsFactory(100), { portion: 10, delay: delayStub });

        expect(delayStub.mock.calls[0][0]).toEqual(10);
        expect(delayStub.mock.calls[8][0]).toEqual(90);
        expect(delayStub).toHaveBeenCalledTimes(9);
    });
});

describe('принимает в качестве processor функцию', () => {
    it('вызывает ее с аргументом запроса', async () => {
        const processorStub = jest.fn();

        await throttler(requestsFactory(2), { portion: 10, processor: processorStub });

        expect(processorStub).toHaveBeenCalledTimes(2);
        expect(processorStub.mock.calls[0][0]).toEqual(0);
        expect(processorStub.mock.calls[1][0]).toEqual(1);
    });
});
