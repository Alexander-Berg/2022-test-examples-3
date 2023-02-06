'use strict';

jest.mock('../_helpers/has-recent-auth');
afterAll(() => jest.unmock('../_helpers/has-recent-auth'));

const hasRecentAuth = require('../_helpers/has-recent-auth');
jest.unmock('@yandex-int/duffman');
const { CUSTOM_ERROR, HTTP_ERROR } = require('@yandex-int/duffman').errors;

const model = require('./do-backup-create');

describe('models/do-backup-create', () => {
    let core;
    let barbet;

    const mockBackup = {
        primary: {
            backup_id: '1'
        }
    };

    const barbetResponse = {
        ...mockBackup,
        secondary: null
    };

    beforeEach(() => {
        barbet = jest.fn(() => Promise.resolve(barbetResponse));
        core = {
            request: jest.fn(() => ({})),
            service: jest.fn(() => barbet)
        };
        hasRecentAuth.mockReturnValue(true);
    });

    it('should not check auth creating fresh backup', async () => {
        await model({}, core);
        expect(core.request).toBeCalledWith('backup-get-status');
        expect(hasRecentAuth).not.toBeCalled();
    });

    it('should check auth rewriting backup', async () => {
        core.request = jest.fn(() => mockBackup);

        await model({}, core);
        expect(hasRecentAuth).toBeCalledWith(core);
    });

    it('should throw error when no recent auth', async () => {
        core.request = jest.fn(() => mockBackup);
        hasRecentAuth.mockReturnValue(Promise.resolve(false));

        let err;

        try {
            await model({}, core);
        } catch (e) {
            err = e;
        }
        expect(err).toEqual(new CUSTOM_ERROR({ code: 'NO_AUTH', message: 'Not authenticated' }));
    });

    it('should call barbet\'s create backup', async () => {
        const response = await model({}, core);
        expect(barbet).toBeCalledWith('/backup/create', {}, { method: 'post' });
        expect(response).toBe(barbetResponse);
    });

    it('should return custom error on http failure', async () => {
        barbet.mockReturnValue(Promise.reject(new HTTP_ERROR({
            body: {
                message: 'message',
                reason: 'reason',
                category: 'category',
                other: 'other'
            }
        })));

        let err;
        try {
            await model({}, core);
        } catch (e) {
            err = e;
        }

        expect(err).toEqual(new CUSTOM_ERROR({
            code: 0,
            type: 'http',
            message: 'message',
            reason: 'reason',
            category: 'category'
        }));
    });

    it('should rethrow unknown error', async () => {
        const expectedError = new Error('unknown');
        barbet.mockReturnValue(Promise.reject(expectedError));

        let err;
        try {
            await model({}, core);
        } catch (e) {
            err = e;
        }

        expect(err).toBe(expectedError);
    });
});
