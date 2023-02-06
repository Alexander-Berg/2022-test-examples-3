/* eslint-disable @typescript-eslint/no-explicit-any */

import { Request } from 'express';

import { middlewarePromisify } from '../middlewarePromisify';

describe('middlewarePromisify', () => {
    test('return req', async () => {
        const middleware = (_req: Request, _res: any, next: any) => {
            next();
        };

        const req = {} as Request;
        const result = await middlewarePromisify(middleware)(req, null);

        expect(result).toBe(req);
    });

    test('throw error if next with error', async () => {
        const middleware = (_req: Request, _res: any, next: any) => {
            next(new Error('some error'));
        };

        const req = {} as Request;

        try {
            await middlewarePromisify(middleware)(req, null);
            expect(true).toBe(false);
        } catch (err) {
            expect(err).toBeTruthy();
        }
    });

    test('throw error if middleware throw error', async () => {
        const middleware = (_req: Request, _res: any, _next: any) => {
            throw new Error('some error');
        };

        const req = {} as Request;

        try {
            await middlewarePromisify(middleware)(req, null);
            expect(true).toBe(false);
        } catch (err) {
            expect(err).toBeTruthy();
        }
    });
});
