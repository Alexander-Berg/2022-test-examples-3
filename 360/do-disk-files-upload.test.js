'use strict';

const { NO_IDS } = require('@ps-int/mail-lib').errors;

const mockDrop = jest.fn();
jest.mock('./do-disk-files-drop', () => mockDrop);

const model = require('./do-disk-files-upload');

let core;
let mockDisk;

beforeEach(() => {
    mockDisk = jest.fn();
    core = {
        service: () => mockDisk
    };
});

describe('возвращает NO_IDS', () => {
    it('если не передан mid и mailAttaches и diskAttaches', async () => {
        try {
            await model({}, core);
            throw new Error('MUST REJECT');
        } catch (err) {
            expect(err).toBeInstanceOf(NO_IDS);
        }
    });
});

describe('если переданы mid и mailAttaches и path', () => {
    const params = {
        path: '/disk/foo',
        mid: '42',
        mailAttaches: [
            { hid: '1', name: 'foo' },
            { hid: '2', name: 'bar' },
            { hid: '3', name: 'baz' }
        ]
    };

    it('не получает папку загрузок', async () => {
        mockDisk.mockResolvedValue({});

        await model(params, core);

        expect(mockDisk.mock.calls[0][0]).not.toBe('/default_folders');
    });
});

describe('если переданы mid и mailAttaches', () => {
    const params = {
        mid: '42',
        mailAttaches: [
            { hid: '1', name: 'foo' },
            { hid: '2', name: 'bar' },
            { hid: '3', name: 'baz' }
        ]
    };

    it('получает папку загрузок', async () => {
        mockDisk.mockResolvedValue({});

        await model(params, core);

        expect(mockDisk.mock.calls[0][0]).toBe('/default_folders');
    });

    it('возвращает DOWNLOADING пока есть незаконченные операции', async () => {
        mockDisk.mockResolvedValueOnce({})
            .mockResolvedValueOnce({ status: 'EXECUTING' })
            .mockResolvedValueOnce({ status: 'WAITING' });

        const res = await model(params, core);

        expect(res.status).toBe('DOWNLOADING');
    });
});

describe('если переданы mid, mailAttaches и diskAttaches', () => {
    const params = {
        mid: '42',
        mailAttaches: [
            { hid: '1', name: 'foo' },
            { hid: '2', name: 'bar' },
            { hid: '3', name: 'baz' }
        ],
        diskAttaches: [
            { hash: 'deadbeef1' },
            { hash: 'defbeef42' }
        ]
    };

    it('вызывает doDiskFilesDrop если хотябы одна операция зафейлилась', async () => {
        mockDisk.mockResolvedValueOnce({})
            .mockResolvedValueOnce({ status: 'error', error: {} })
            .mockResolvedValueOnce({ oid: '1', target_path: '/qwe/asd', type: 'copy' })
            .mockResolvedValueOnce({ oid: '2', target_path: '/qwe/asd', type: 'copy' });
        mockDrop.mockResolvedValueOnce({ status: 'REMOVED' });

        const res = await model(params, core);

        expect(mockDrop).toBeCalledWith({
            oids: [ '1', '2' ]
        }, core);
        expect(res.status).toBe('REMOVED');
    });
});

describe('если передан пустой mailAttaches', () => {
    const params = {
        mid: '42',
        path: '/path',
        mailAttaches: []
    };

    it('не ходит в диск', async () => {
        mockDisk.mockResolvedValue({});

        await model(params, core);

        expect(mockDisk.mock.calls).toHaveLength(0);
    });
});

describe('если переданы mid и diskAttaches', () => {
    const params = {
        mid: '42',
        diskAttaches: [
            { hash: '1' },
            { hash: '2' },
            { hash: '3' }
        ]
    };

    it('возвращает DOWNLOADING пока есть незаконченные операции', async () => {
        mockDisk
            .mockResolvedValueOnce({ status: 'EXECUTING' })
            .mockResolvedValueOnce({ status: 'WAITING' })
            .mockResolvedValue({});

        const res = await model(params, core);

        expect(res.status).toBe('DOWNLOADING');
    });

    it('вызывает doDiskFilesDrop если хотябы одна операция зафейлилась', async () => {
        mockDisk
            .mockResolvedValueOnce({})
            .mockResolvedValueOnce({ oid: '1', target_path: '/qwe/asd', type: 'copy' })
            .mockResolvedValueOnce({ status: 'error', error: {} })
            .mockResolvedValueOnce({ oid: '2', target_path: '/qwe/asd', type: 'copy' });
        mockDrop.mockResolvedValueOnce({ status: 'REMOVED' });

        const res = await model(params, core);

        expect(mockDrop).toBeCalledWith({
            oids: [ '1', '2' ]
        }, core);
        expect(res.status).toBe('REMOVED');
    });
});

describe('path', () => {
    const params = {
        mid: '42',
        mailAttaches: [ { hid: '1', name: 'foo' } ],
        diskAttaches: [ { hash: '1' } ]
    };

    it('если не передан, получает path из default_folders', async () => {
        mockDisk.mockImplementation((arg) => {
            if (arg === '/default_folders') {
                return Promise.resolve({ downloads: '/disk/path/' });
            }
            if (arg === '/json/import_attaches_to_disk') {
                return Promise.resolve({});
            }
            if (arg === '/json/async_public_copy') {
                return Promise.resolve({});
            }
        });

        await model(params, core);

        expect(mockDisk.mock.calls[1][1].save_path).toBe('/disk/path');
        expect(mockDisk.mock.calls[2][1].path).toBe('/disk/path');
    });

    it('если не передан и default_folders фейлится, используем дефолтный', async () => {
        mockDisk.mockImplementation((arg) => {
            if (arg === '/default_folders') {
                return Promise.reject({});
            }
            if (arg === '/json/import_attaches_to_disk') {
                return Promise.resolve({});
            }
            if (arg === '/json/async_public_copy') {
                return Promise.resolve({});
            }
        });

        await model(params, core);

        expect(mockDisk.mock.calls[1][1].save_path).toBe('/disk/Загрузки');
        expect(mockDisk.mock.calls[2][1].path).toBe('/disk/Загрузки');
    });

    it('если передан, использует его', async () => {
        mockDisk.mockImplementation((arg) => {
            if (arg === '/json/import_attaches_to_disk') {
                return Promise.resolve({});
            }
            if (arg === '/json/async_public_copy') {
                return Promise.resolve({});
            }
        });

        await model({ ...params, path: '/disk/bar' }, core);

        expect(mockDisk.mock.calls).toHaveLength(2);
        expect(mockDisk.mock.calls[0][1].save_path).toBe('/disk/bar');
        expect(mockDisk.mock.calls[1][1].path).toBe('/disk/bar');
    });
});
