/* eslint-disable */
import { Assertions } from 'ava';
import * as request from 'supertest';
import { makeAppOnce } from '../../../../../makeApp';

export const callApi = (method: string, params: any[] = []) => {
    return request(makeAppOnce({ startPgPool: false, shouldEnableUpdatableCache: false }))
        .post('/api/external/v1')
        .send({ jsonrpc: '2.0', id: 1, method, params });
};

export const respondsWithError = (error: any, res: request.Response, t: Assertions) => {
    t.is(res.status, 200);
    t.deepEqual(res.body, { jsonrpc: '2.0', id: 1, error });
};

export const respondsWithResult = (result: any, res: request.Response, t: Assertions) => {
    t.is(res.status, 200);
    t.deepEqual(res.body, { jsonrpc: '2.0', id: 1, result });
};
