'use strict';

const moveToFolder = require('./move_to_folder.js');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const status = require('../_helpers/status');

let core;
let mockMeta;
let mockMops;

beforeEach(() => {
    mockMeta = jest.fn();
    mockMops = jest.fn();
    core = {
        params: {},
        service: (service) => service === 'mops' ? mockMops : mockMeta
    };
    core.status = status(core);
});

test('-> PERM_FAIL без параметров', async () => {
    const res = await moveToFolder(core);

    expect(res.status).toBe(3);
    expect(res.phrase).toInclude('invalid params schema');
});

describe('параметры, уходящие в сервис правильные', () => {
    beforeEach(() => {
        core.params.fid = '1';
        mockMops.mockResolvedValueOnce({});
    });

    it('метод', async () => {
        core.params.tids = '42';

        await moveToFolder(core);

        expect(mockMops.mock.calls[0][0]).toBe('/complex_move');
    });

    it('tids', async () => {
        core.params.tids = '42,43';

        await moveToFolder(core);

        expect(mockMops.mock.calls[0][1].tids).toBe('42,43');
    });

    it('mids', async () => {
        core.params.mids = '43,44,45';

        await moveToFolder(core);

        expect(mockMops.mock.calls[0][1].mids).toBe('43,44,45');
    });
});

test('-> OK', async () => {
    core.params.fid = '42';
    core.params.tids = '42';
    mockMops.mockResolvedValueOnce({});

    const result = await moveToFolder(core);

    expect(result).toEqual({ status: 1 });
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.fid = '42';
        core.params.tids = '42';
    });

    it('-> PERM_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(400));

        const result = await moveToFolder(core);

        expect(result.status).toBe(3);
    });

    it('-> TMP_FAIL', async () => {
        mockMops.mockRejectedValueOnce(httpError(500));

        const result = await moveToFolder(core);

        expect(result.status).toBe(2);
    });
});

describe('если указан параметр tab', () => {
    beforeEach(() => {
        core.params = { tab: 'news', mids: '1,23', tids: '1234' };
        mockMops.mockResolvedValueOnce({});
        mockMeta.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    });

    it('невалидный таб -> ошибка', async () => {
        core.params.tab = 'blah';

        const res = await moveToFolder(core);

        expect(res.status).toBe(3);
        expect(res.phrase).toInclude('data.tab should be equal to one of the allowed values');
    });

    it('ok', async () => {
        await moveToFolder(core);

        expect(mockMops.mock.calls[0][0]).toBe('/complex_move');
        expect(mockMops.mock.calls[0][1]).toEqual({
            mids: '1,23',
            dest_fid: '1',
            dest_tab: 'news',
            tids: '1234'
        });
    });
});
