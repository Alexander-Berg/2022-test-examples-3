/* eslint-disable */
import * as request from 'supertest';
import { makeAppOnce } from '../../../../makeApp';

export const callApi = (path: string, body?: any) => {
    const url = '/api/scheduler/v1/' + path;
    return request(makeAppOnce({ startPgPool: false, shouldEnableUpdatableCache: false }))
        .post(url)
        .send(body);
};
