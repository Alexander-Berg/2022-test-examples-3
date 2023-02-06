'use strict';

jest.spyOn(global, 'setTimeout');

const mockFilter = jest.fn();
const mockService = jest.fn();
jest.mock('./_filter.js', () => mockFilter);

const translateLangs = require('./translate-langs.js');

const core = {
    config: {
        locale: 'FAKE_LOCALE'
    },
    service: () => mockService
};

const TTL = 20 * 60 * 1000;

test('1 вызов', async () => {
    jest.useFakeTimers('legacy');
    const data = { foo: 'bar' };
    mockService.mockResolvedValueOnce(data);
    mockFilter.mockReturnValueOnce('FILTERED_DATA');

    const res = await translateLangs({}, core);

    expect(setTimeout).toHaveBeenCalledTimes(1);
    expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), TTL);

    expect(res).toBe('FILTERED_DATA');
    expect(mockFilter).toBeCalledWith(data);
    expect(mockService).toBeCalledWith('/getLangs', { ui: 'FAKE_LOCALE' }, { form: true });
});

test('2 вызов, берем из кеша', async () => {
    const res = await translateLangs({}, core);

    expect(res).toBe('FILTERED_DATA');
    expect(mockFilter).not.toBeCalled();
    expect(mockService).not.toBeCalled();
});

test('3 вызов, кеш протух', async () => {
    jest.useFakeTimers('legacy');
    const data = { foo: 'boo' };
    mockService.mockResolvedValueOnce(data);
    mockFilter.mockReturnValueOnce('FILTERED_DATA2');

    jest.advanceTimersByTime(TTL);

    const res = await translateLangs({}, core);

    expect(res).toBe('FILTERED_DATA2');
    expect(mockFilter).toBeCalledWith(data);
    expect(mockService).toBeCalledWith('/getLangs', { ui: 'FAKE_LOCALE' }, { form: true });
});

test('4 вызов, другая локаль, идем в сервис', async () => {
    jest.useFakeTimers('legacy');
    mockService.mockResolvedValueOnce({});
    mockFilter.mockReturnValueOnce('FILTERED_DATA3');
    core.config.locale = 'FAKE_LOCALE2';

    const res = await translateLangs({}, core);

    expect(res).toBe('FILTERED_DATA3');
    expect(mockFilter).toBeCalledWith({});
    expect(mockService).toBeCalledWith('/getLangs', { ui: 'FAKE_LOCALE2' }, { form: true });
});
