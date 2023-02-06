'use strict';

const createLabel = require('./create_label.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockMops;

beforeEach(() => {
    mockMops = jest.fn();
    core = {
        params: {},
        service: () => mockMops
    };
    core.status = status(core);
});

describe('-> PERM_FAIL по параметрам', () => {
    it('без name и symbol', async () => {
        const res = await createLabel(core);

        expect(res.status.status).toBe(3);
        expect(res.status.phrase).toInclude('name or symbol param is missing');
    });

    it('одновременно name и symbol', async () => {
        core.params = { name: '1', symbol: '2' };

        const res = await createLabel(core);

        expect(res.status.status).toBe(3);
        expect(res.status.phrase).toInclude('You cannot pass both symbol and name parametrs');
    });

});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        mockMops.mockResolvedValueOnce([]);
        core.params.name = 'label42';
    });

    it('метод правильный', async () => {
        await createLabel(core);

        expect(mockMops.mock.calls[0][0]).toEqual('/labels/create');
    });

    it('цвет парсится', async () => {
        core.params.color = 'ffcc00';

        await createLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.color).toBe(16763904);
    });

    it('нули в начале', async () => {
        core.params.color = '00ffcc';

        await createLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.color).toBe(65484);
    });

    it('цвет по-умолчанию 0', async () => {
        await createLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.color).toBe(0);
    });

    it('name', async () => {
        await createLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.name).toBe('label42');
    });

    it('symbol', async () => {
        delete core.params.name;
        core.params.symbol = 'symbol42';

        await createLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.symbol).toBe('symbol42');
    });

    it('type', async () => {
        core.params.type = 'type42';

        await createLabel(core);

        const params = mockMops.mock.calls[0][1];
        expect(params.type).toBe('type42');
    });

});

test('-> OK', () => {
    core.params.name = '1';
    mockMops.mockResolvedValueOnce();
    return createLabel(core).then((result) => {
        expect(result.status.status).toBe(1);
    });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.name = '1';
    });

    it('-> PERM_FAIL', () => {
        mockMops.mockRejectedValueOnce(httpError(400));
        return createLabel(core).then((result) => {
            expect(result.status.status).toBe(3);
        });
    });

    it('-> TMP_FAIL', () => {
        mockMops.mockRejectedValueOnce(httpError(500));
        return createLabel(core).then((result) => {
            expect(result.status.status).toBe(2);
        });
    });
});
