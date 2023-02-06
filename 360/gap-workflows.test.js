'use strict';

const model = require('./gap-workflows');

let core;
const staffService = jest.fn();

const workflowsMock = {
    workflows: [ {
        type: 'absence',
        color: '#ffc136',
        verbose_name: {
            ru: 'Отсутсвие',
            en: 'Absence'
        }
    } ]
};

beforeEach(() => {
    staffService.mockResolvedValue(workflowsMock);

    core = {
        service: () => staffService
    };
    model.clearCache();
});

test('идёт в сервис', async () => {
    await model({}, core);
    expect(staffService).toHaveBeenCalledWith('/gap-api/api/workflows/', {}, { method: 'GET' });
});

test('не идёт в сервис если кеш валидный', async () => {
    await model({}, core);
    await model({}, core);
    expect(staffService).toHaveBeenCalledTimes(1);
});

test('возвращает данные из кеша в случае ошибки', async () => {
    const dateNowSpy = jest.spyOn(Date, 'now').mockImplementation(() => 1500000000000);
    await model({}, core);

    dateNowSpy.mockImplementation(() => 1600000000000);
    staffService.mockRejectedValueOnce('error');
    const result = await model({}, core);

    expect(result).toEqual({
        absence: workflowsMock.workflows[0]
    });
    expect(staffService).toHaveBeenCalledTimes(2);

    dateNowSpy.mockRestore();
});

test('кидает ошибку, если в кеше пусто', async () => {
    staffService.mockRejectedValueOnce('error');
    expect.hasAssertions();
    try {
        await model({}, core);
    } catch (e) {
        expect(e).toBe('error');
    }
});
