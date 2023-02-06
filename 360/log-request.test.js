'use strict';

jest.mock('../lib/request-logger.js');

const httpMocks = require('node-mocks-http');
const { EventEmitter } = require('events');
const RequestLogger = require('../lib/request-logger.js');
const logRequest = require('./log-request.js');

const info = RequestLogger.mock.instances[0].info;
const next = jest.fn();

describe('middlewares', () => {
    it('calls requestLogger.info', () => {
        const req = httpMocks.createRequest({
            method: 1,
            originalUrl: 2,
            headers: {
                'user-agent': 3,
                'x-original-uri': 4,
                'x-real-ip': 5,
                'x-request-id': 6
            },
            hostname: 7,
            query: { client_name: 8, uid: 10 },
            cookies: { yandexuid: 9 }
        });
        const res = httpMocks.createResponse({
            eventEmitter: EventEmitter
        });
        res.setHeader('location', 11);

        logRequest(req, res, next);

        expect(next).toHaveBeenCalled();

        return new Promise((resolve) => {
            res.on('finish', resolve).end();
        }).then(() => {
            expect(info).toHaveBeenCalledWith('REQUEST_FINISHED', {
                reason: 'REQUEST_FINISHED',
                method: 1,
                request: 2,
                user_agent: 3,
                status: 200,
                livetime: expect.stringMatching(/^\d+(\.\d+)?ms$/),
                redirect_to: 11,
                x_original_uri: 4,
                x_real_ip: 5,
                x_request_id: 6,
                host: 7,
                project: 8,
                yandexuid: 9,
                uid: 10
            });
        });
    });

    it('falls back to uid from req.core', () => {
        const req = httpMocks.createRequest({
            core: { auth: { get: jest.fn().mockReturnValue({ uid: 1 }) } }
        });
        const res = httpMocks.createResponse({
            eventEmitter: EventEmitter
        });
        logRequest(req, res, next);

        expect(next).toHaveBeenCalled();

        return new Promise((resolve) => {
            res.on('finish', resolve).end();
        }).then(() => {
            expect(info.mock.calls[0][1].uid).toBe(1);
        });
    });

    it('falls back to empty uid', () => {
        const req = httpMocks.createRequest({});
        const res = httpMocks.createResponse({
            eventEmitter: EventEmitter
        });
        logRequest(req, res, next);

        expect(next).toHaveBeenCalled();

        return new Promise((resolve) => {
            res.on('finish', resolve).end();
        }).then(() => {
            expect(info.mock.calls[0][1].uid).toBe('');
        });
    });
});
