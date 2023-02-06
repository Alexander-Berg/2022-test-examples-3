/* eslint-disable */
import { makeRestApiCaller, getServiceTicket } from '../_helpers';
import { User } from '../../../../db';

export const makeGetRequest = async(path: string) => {
    const serviceTicket = await getServiceTicket();
    return makeRestApiCaller('idm/')('get', path, {
        contentType: 'application/x-www-form-urlencoded',
        serviceTicket,
    });
};

export const makePostRequest = async(path: string, reqProps: object) => {
    const serviceTicket = await getServiceTicket();
    return makeRestApiCaller('idm/')('post', path, {
        contentType: 'application/x-www-form-urlencoded',
        serviceTicket,
    }).send(reqProps);
};

export const getUserById = async(id: string) => {
    const user = await User.findByPk(id, {
        rejectOnEmpty: true,
    });

    return user!;
};
