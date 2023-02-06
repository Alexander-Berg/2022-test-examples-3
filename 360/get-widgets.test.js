'use strict';

const getWidgets = require('./get-widgets.js');

let core;
let taksa;

beforeEach(() => {
    taksa = jest.fn().mockResolvedValue({});
    core = {
        services: {
            taksa
        },
        service: jest.fn().mockReturnValue(taksa),
        params: {
            withWidgets: '1'
        },
        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        }
    };
});

afterEach(() => {
    taksa.mockRestore();
});

test('работает с промисом', async () => {
    const data = Promise.resolve({ envelopes: [ 'promise_data' ] });

    await getWidgets(core, { data });
    const args = taksa.mock.calls[0];

    expect(args[0]).toEqual('/api/list');
    expect(args[1]).toEqual({ envelopes: [ 'promise_data' ] });
});

test('работает с объектом', async () => {
    const data = { envelopes: [ 'data' ] };

    await getWidgets(core, { data });
    const args = taksa.mock.calls[0];

    expect(args[0]).toEqual('/api/list');
    expect(args[1]).toEqual({ envelopes: [ 'data' ] });
});

test('фильтрует ответ сервиса', async () => {
    const data = { threads_by_folder: { envelopes: [ 'data' ] } };
    taksa.mockResolvedValueOnce({ widgets: [ { info: { foo: 'bar' }, contols: { qwe: 'asd ' } } ] });

    const res = await getWidgets(core, { data });

    expect(res).toEqual([ { info: { foo: 'bar' } } ]);
});

test('ходит в сервис с нужными параметрами', async () => {
    const data = { threads_by_folder: { envelopes: [ 'data' ] } };
    taksa.mockResolvedValueOnce({});

    await getWidgets(core, { data });

    expect(taksa.mock.calls[0]).toMatchSnapshot();
});

describe('возвращает пустой массив', () => {
    beforeEach(() => {
        delete core.params.withWidgets;
    });

    it('если не пришел параметр withWidgets', async () => {
        const data = { envelopes: [ 'data' ] };

        await getWidgets(core, { data });

        expect(taksa).not.toBeCalled();
    });

    it('если сервис зареджектился', async () => {
        const data = { threads_by_folder: { envelopes: 'data' } };
        taksa.mockRejectedValueOnce({});

        const res = await getWidgets(core, { data });

        expect(res).toEqual([]);
    });

    it('если писем нет', async () => {
        const data = { envelopes: [] };

        const res = await getWidgets(core, { data });

        expect(res).toEqual([]);
    });

    it('если промис в параметрах брякнулся', async () => {
        core.params.withWidgets = '1';
        const data = Promise.reject();

        const res = await getWidgets(core, { data });

        expect(res).toEqual([]);
    });
});
