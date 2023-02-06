/* eslint-disable */
import * as request from 'supertest';
import { makeAppOnce } from '../../../makeApp';
import { getOauth } from '../../../services/blackbox';
import { getServiceTickets } from '../../../services/tvm';
import { User } from '../../../db';

export const getServiceTicket = async() => {
    const serviceTickets = await getServiceTickets(['dialogs']);
    return serviceTickets.dialogs.ticket;
};

export const getUserTicket = async(oauthToken: string) => {
    const response = await getOauth({
        oauth_token: oauthToken,
        get_user_ticket: 'yes',
    });

    return response.body.user_ticket;
};

export type HTTPMethod = 'get' | 'post' | 'put' | 'delete' | 'patch';

export interface CallRestApiOptions {
    userTicket?: string;
    serviceTicket?: string;
    contentType?: string;
}

export type CallRestApi = <T extends CallRestApiOptions>(
    method: HTTPMethod,
    route: string,
    options?: T,
) => request.Test;

export const makeRestApiCaller: (prefix: string) => CallRestApi = prefix => (
    method,
    route,
    options,
) => {
    let res = request(makeAppOnce({ startPgPool: false, shouldEnableUpdatableCache: false }))[
        method
    ](`/api/${prefix}${route}`);

    if (options && options.userTicket) {
        res = res.set('x-ya-user-ticket', options.userTicket);
    }
    if (options && options.serviceTicket) {
        res = res.set('x-ya-service-ticket', options.serviceTicket);
    }
    if (options && options.contentType) {
        res = res.set('Content-Type', options.contentType);
    }
    return res;
};

export const testUser = {
    oauthToken: 'AQAAAADubtmxAAAO7LsvJKeqJEGFki60RJmPqxE',
    uid: '4000242097',
};

export const getUserById = async(id: string) => {
    const user = await User.findByPk(id, {
        rejectOnEmpty: true,
    });

    return user!;
};
