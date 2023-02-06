import type {AxiosResponse} from 'axios';

import {api, ApiParams, Headers} from '~/testServer';

export type RequestToHandlerParams = Partial<ApiParams<unknown>>;
export type RequestToHandler<P> = (params: Partial<P>, headers?: Headers) => Promise<AxiosResponse>;

export const makeRequestToHandler =
    (host: string, name: string, version: number = 1) =>
    (params: unknown, headers: Headers = {}): Promise<AxiosResponse<unknown>> =>
        api({host, params, name, version, headers}).catch(error => error);
