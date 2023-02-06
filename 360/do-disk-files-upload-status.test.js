'use strict';

const { NO_IDS } = require('@ps-int/mail-lib').errors;

const mockDrop = jest.fn();
jest.mock('./do-disk-files-drop', () => mockDrop);

const model = require('./do-disk-files-upload-status');

let core;
let mockDisk;

beforeEach(() => {
    mockDisk = jest.fn();
    core = {
        service: () => mockDisk
    };
});

test('возвращает NO_IDS, если не переданы oids', async () => {
    try {
        await model({}, core);
        throw new Error('MUST REJECT');
    } catch (err) {
        expect(err).toBeInstanceOf(NO_IDS);
    }
});

describe('если переданы oids', () => {
    it('возвращает DOWNLOADING пока есть незаконченные операции', async () => {
        mockDisk
            .mockResolvedValueOnce({ status: 'EXECUTING' })
            .mockResolvedValue({ status: 'WAITING' });

        const res = await model({ oids: [ '1', '2', '3' ] }, core);

        expect(res.status).toBe('DOWNLOADING');
    });

    it('возвращает DONE когда все операции закончились', async () => {
        mockDisk.mockResolvedValue({ status: 'DONE' });

        const res = await model({ oids: [ '1', '2', '3' ] }, core);

        expect(res.status).toBe('DONE');
    });

    [ 'FAILED', 'ABORTED', 'REJECTED' ].forEach((status) => {
        it(`вызывает doDiskFilesDrop если хотябы одна операция ${status}`, async () => {
            mockDisk.mockResolvedValueOnce({ status })
                .mockResolvedValue({ status: 'DONE', resource: { path: 'foo' } });
            mockDrop.mockResolvedValue({ status: 'REMOVED' });

            const res = await model({ oids: [ '1', '2', '3' ] }, core);

            expect(mockDrop).toBeCalledWith({
                oids: [ '1', '2', '3' ]
            }, core);
            expect(res.status).toBe('REMOVED');
        });
    });

    it('вызывает doDiskFilesDrop если хотябы одна операция зареджектилась noFreeSpace', async () => {
        mockDisk.mockRejectedValueOnce(require('./_mock/disk-no-free-space.json'))
            .mockResolvedValue({ status: 'DONE', resource: { path: 'foo' } });
        mockDrop.mockResolvedValue({ status: 'REMOVED' });

        const res = await model({ oids: [ '1', '2', '3' ] }, core);

        expect(mockDrop).toBeCalledWith({
            oids: [ '1', '2', '3' ]
        }, core);
        expect(res.status).toBe('REMOVED');
    });

    it('вызывает doDiskFilesDrop если хотябы одна операция зареджектилась noFreeSpaceCopyToDisk', async () => {
        mockDisk.mockRejectedValueOnce(require('./_mock/disk-no-free-space-copy-to-disk.json'))
            .mockResolvedValue({ status: 'DONE', resource: { path: 'foo' } });
        mockDrop.mockResolvedValue({ status: 'REMOVED' });

        const res = await model({ oids: [ '1', '2', '3' ] }, core);

        expect(mockDrop).toBeCalledWith({
            oids: [ '1', '2', '3' ]
        }, core);
        expect(res.status).toBe('REMOVED');
    });
});
