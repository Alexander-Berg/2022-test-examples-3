import axios from 'axios';
import stout from '@yandex-market/stout';
import {promisifyResponse} from '@yandex-market/mandrel/lib/helpers';

import {serverInit} from '~/configs/jest/mocks/luster';

import '~/app';

export {setBackendHandler, getBackendHandler, BackendResponse} from '~/configs/jest/mocks/mandrel/base/backendHandlers';
export {defaultBackendHandler} from './defaultBackendHandler';
export {
    default as blackboxMockResponse,
    CURRENT_USER_ID,
    CURRENT_USER_LOGIN,
} from './defaultBackendHandler/mockResponse/blackbox';

export const startServer = (): Promise<string> =>
    serverInit.then(() => `http://localhost:${stout?.data?.config?.server?.port}`);
export const stopServer = (): Promise<void> => promisifyResponse(stout.stopServer());

export type Headers = Record<string, string | number | boolean>;
export type ApiParams<P> = {
    host: string;
    name: string;
    version?: number;
    params: P;
    headers?: Headers;
};

export const api = <P = Record<string, unknown>, R = Record<string, unknown>>({
    host,
    name,
    params,
    version = 1,
    headers,
}: ApiParams<P>): Promise<R> =>
    axios.post(
        `${host}/api/v${version}?name=${name}`,
        {params: [params]},
        {
            headers: {
                ...headers,
                'Content-Type': 'application/json',
                authorization: 'OAuth token',
            },
        },
    );
