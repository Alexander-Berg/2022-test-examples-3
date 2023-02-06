/* eslint-disable */
import { Assertions } from 'ava';
import { pick } from 'lodash';
import * as request from 'supertest';
import { makeRestApiCaller } from '../_helpers';

export const callApi = makeRestApiCaller('dev-console/v1');

export const respondsWithError = (
    {
        code,
        message,
        payload,
        ...fields
    }: {
        code: number;
        message: string;
        payload?: any;
        [k: string]: any;
    },
    res: request.Response,
    t: Assertions,
) => {
    t.is(res.status, code);
    t.deepEqual(res.body, {
        error: { message, code, ...fields },
        ...(payload ? { payload } : undefined),
    });
};

export const respondsWithCreatedModelContains = (props: any, res: request.Response, t: Assertions) => {
    res.body.result = res.body.result && pick(res.body.result, Object.keys(props));

    t.is(res.status, 201);
    t.deepEqual(res.body, { result: props });
};

export const respondsWithCreatedModel = (props: any, res: request.Response, t: Assertions) => {
    t.is(res.status, 201);
    t.deepEqual(res.body, { result: props });
};

export const respondsWithExistingModelContains = (props: any, res: request.Response, t: Assertions) => {
    res.body.result = res.body.result && pick(res.body.result, Object.keys(props));

    t.is(res.status, 200);
    t.deepEqual(res.body, { result: props });
};

export const respondsWithExistingModel = (props: any, res: request.Response, t: Assertions) => {
    t.is(res.status, 200);
    t.deepEqual(res.body, { result: props });
};
