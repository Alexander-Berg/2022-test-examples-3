/* eslint-disable */
import { Assertions } from 'ava';
import * as request from 'supertest';
import { makeRestApiCaller } from '../../_helpers';

export const callApi = makeRestApiCaller('external/v2');

export const respondsWithError = (errorCode: number, errorMessage: string, res: request.Response, t: Assertions) => {
    t.is(res.status, errorCode);
    t.deepEqual(res.body, { error: { message: errorMessage, code: errorCode } });
};

export const respondsWithResult = (result: any, res: request.Response, t: Assertions, status: number = 200) => {
    t.is(res.status, status);
    t.deepEqual(res.body, result);
};

export const respondsWithResultBulk = (result: any, res: request.Response, t: Assertions) => {
    t.is(res.status, 200);
    t.deepEqual(res.body, result);
};
