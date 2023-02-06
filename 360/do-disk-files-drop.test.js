'use strict';

const { NO_IDS } = require('@ps-int/mail-lib').errors;

const model = require('./do-disk-files-drop');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: () => mockService
    };
});

describe('возвращает NO_IDS', () => {
    it('если не передан oids', async () => {
        try {
            await model({}, core);
            throw new Error('MUST REJECT');
        } catch (err) {
            expect(err).toBeInstanceOf(NO_IDS);
        }
    });
});

describe('если переданы oids', () => {
    const params = { oids: [ '1', '2', '3' ] };

    it('возвращает REMOVING пока есть незаконченные операции', async () => {
        mockService
            .mockResolvedValueOnce({ status: 'EXECUTING' })
            .mockResolvedValue({ status: 'WAITING' });

        const res = await model(params, core);

        expect(res.status).toBe('REMOVING');
    });

    it('возвращает REMOVED когда все выполнились', async () => {
        mockService.mockResolvedValue({ status: 'DONE', resource: { path: 'qwe' } });

        const res = await model(params, core);

        expect(res.status).toBe('REMOVED');
    });
});
