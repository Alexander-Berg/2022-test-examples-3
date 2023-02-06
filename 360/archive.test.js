'use strict';

const archive = require('./archive.js');
const status = require('../_helpers/status');

let core;
let mockMops;
let mockMeta;

beforeEach(() => {
    mockMops = jest.fn();
    mockMeta = jest.fn();
    core = {
        params: {},
        service: (service) => service === 'mops' ? mockMops : mockMeta
    };
    core.status = status(core);
});

test('-> PERM_FAIL без нужных параметров', async () => {
    const res = await archive(core);

    expect(res.status.status).toBe(3);
});

describe('если нет папки', () => {
    beforeEach(() => {
        core.params.mids = '1,2,3';
        core.params.local = 'архив';
        mockMeta.mockResolvedValueOnce({ folders: {} });
    });

    it('дергает meta c правильными параметрами', async () => {
        mockMops.mockResolvedValue({ fid: '42' });

        await archive(core);

        expect(mockMeta).toHaveBeenCalledWith('/folders');
        expect(mockMops.mock.calls).toMatchSnapshot();
    });

    it('-> PERM_FAIL если не удалось сделать папку', async () => {
        const result = await archive(core);

        expect(result.status.status).toBe(3);
        expect(result.status.phrase).toInclude('get archive fid');
    });
});

describe('если папка есть', () => {
    beforeEach(() => {
        core.params.tids = '41,42,43';
        core.params.local = 'архив';
        mockMeta.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
        mockMops.mockResolvedValueOnce();
    });

    it('дергает mops c правильными параметрами', async () => {
        await archive(core);

        expect(mockMops).toHaveBeenCalledWith('/complex_move', {
            tids: '41,42,43',
            dest_fid: '9'
        });
    });

    it('-> OK', async () => {
        const result = await archive(core);

        expect(result).toEqual({ status: { status: 1 }, fid: '9' });
    });
});

describe('если папка есть с локализованным названием', () => {
    beforeEach(() => {
        core.params.tids = '41,42,43';
        core.params.local = 'архив';
        mockMeta.mockResolvedValueOnce(require('../../../test/mock/folders-custom-archive.json'));
        mockMops.mockResolvedValue();
    });

    it('дергает mops c правильными параметрами', async () => {
        await archive(core);

        expect(mockMops).toHaveBeenCalledTimes(2);
        expect(mockMops.mock.calls).toMatchSnapshot();
    });

    it('-> OK', async () => {
        const result = await archive(core);

        expect(result).toEqual({ status: { status: 1 }, fid: '42' });
    });
});

describe('если папка есть с названием Archive', () => {
    beforeEach(() => {
        core.params.tids = '41,42,43';
        core.params.local = 'архив';
        mockMeta.mockResolvedValueOnce(require('../../../test/mock/folders-archive-name.json'));
        mockMops.mockResolvedValue();
    });

    it('дергает mops c правильными параметрами', async () => {
        await archive(core);

        expect(mockMops).toHaveBeenCalledTimes(2);
        expect(mockMops.mock.calls).toMatchSnapshot();
    });

    it('-> OK', async () => {
        const result = await archive(core);

        expect(result).toEqual({ status: { status: 1 }, fid: '146' });
    });
});

describe('-> PERM_FAIL', () => {
    beforeEach(() => {
        core.params.mids = '1,2,3';
        core.params.local = 'архив';
    });

    it('когда meta ответил ошибкой', async () => {
        mockMeta.mockRejectedValueOnce();
        mockMops.mockRejectedValueOnce();

        const result = await archive(core);

        expect(result.status.status).toBe(3);
        expect(result.status.phrase).toInclude('get archive fid');
    });

    it('когда mops ответил ошибкой', async () => {
        mockMeta.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
        mockMops.mockRejectedValueOnce();

        const result = await archive(core);

        expect(result.status.status).toBe(3);
        expect(result.status.phrase).toInclude('do complex move');
    });
});
