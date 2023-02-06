'use strict';

const model = require('./get-mail-widget-data');

describe('get-mail-widget-data model', () => {
    let core;

    const service = jest.fn();
    beforeEach(function() {
        core = {
            request: jest.fn(),
            service: jest.fn(() => service),
            params: {}
        };
    });

    it('simple request', async () => {
        core.request.mockResolvedValueOnce({ envelopes: [ { from: [ {} ] } ] });
        core.request.mockResolvedValueOnce({ 'somebody <some@email.com>': {} });
        service.mockResolvedValueOnce({ counters: { fresh: 0, unread: 0 } });

        const res = await model({}, core);

        expect(res).toEqual({ counters: { fresh: 0, unread: 0 }, recipients: { 'somebody <some@email.com>': {} } });
    });

    it('should call recipients with right params', async () => {
        core.request.mockResolvedValueOnce({ envelopes: [
            { from: [ { displayName: 'somebody', domain: 'email.com', local: 'some' } ] }
        ] });
        service.mockResolvedValueOnce({ counters: { fresh: 1, unread: 0 } });

        await model({}, core);

        expect(core.request).toHaveBeenCalledWith('recipients', { recipientsIds: [ '"somebody" <some@email.com>' ] });
    });

    it('should call recipients with fresh count mails', async () => {
        core.request.mockResolvedValueOnce({ envelopes: [
            { from: [ { displayName: 'somebody', domain: 'email.com', local: 'some' } ] },
            { from: [ { displayName: 'somebody2', domain: 'email.com', local: 'some2' } ] }
        ] });
        service.mockResolvedValueOnce({ counters: { fresh: 1, unread: 0 } });

        await model({}, core);

        expect(core.request).toHaveBeenCalledWith('recipients', { recipientsIds: [ '"somebody" <some@email.com>' ] });
    });

    it('should call recipients with max 3 mails', async () => {
        core.request.mockResolvedValueOnce({ envelopes: [
            { from: [ { displayName: 'somebody', domain: 'email.com', local: 'some' } ] },
            { from: [ { displayName: 'somebody2', domain: 'email.com', local: 'some2' } ] },
            { from: [ { displayName: 'somebody3', domain: 'email.com', local: 'some3' } ] },
            { from: [ { displayName: 'somebody4', domain: 'email.com', local: 'some4' } ] }
        ] });
        service.mockResolvedValueOnce({ counters: { fresh: 99, unread: 0 } });

        await model({}, core);

        expect(core.request).toHaveBeenCalledWith('recipients', { recipientsIds: [
            '"somebody" <some@email.com>',
            '"somebody2" <some2@email.com>',
            '"somebody3" <some3@email.com>'
        ] });
    });

    it('should call recipients with uniq ids', async () => {
        core.request.mockResolvedValueOnce({ envelopes: [
            { from: [ { displayName: 'somebody', domain: 'email.com', local: 'some' } ] },
            { from: [ { displayName: 'somebody2', domain: 'email.com', local: 'some2' } ] },
            { from: [ { displayName: 'somebody', domain: 'email.com', local: 'some' } ] },
            { from: [ { displayName: 'somebody4', domain: 'email.com', local: 'some4' } ] }
        ] });
        service.mockResolvedValueOnce({ counters: { fresh: 99, unread: 0 } });

        await model({}, core);

        expect(core.request).toHaveBeenCalledWith('recipients', { recipientsIds: [
            '"somebody" <some@email.com>',
            '"somebody2" <some2@email.com>'
        ] });
    });

    it('should not call recipients with undefined', async () => {
        core.request.mockResolvedValueOnce({ envelopes: [
            { },
            { from: [ { displayName: 'somebody2', domain: 'email.com', local: 'some2' } ] },
            { from: [ { displayName: 'somebody', domain: 'email.com', local: 'some' } ] },
            { from: [ { displayName: 'somebody4', domain: 'email.com', local: 'some4' } ] }
        ] });
        service.mockResolvedValueOnce({ counters: { fresh: 99, unread: 0 } });

        await model({}, core);

        expect(core.request).toHaveBeenCalledWith('recipients', { recipientsIds: [
            '"somebody2" <some2@email.com>',
            '"somebody" <some@email.com>'
        ] });
    });

});
