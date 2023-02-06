import {AxiosRequestHeaders} from 'axios';
import forEach from 'lodash/forEach';
import isPlainObject from 'lodash/isPlainObject';
import mapValues from 'lodash/mapValues';

import {CommonHeaders} from 'server/constants/headers';

import {
    ERequestType,
    IBaseRequest,
    TRequest,
} from 'server/utilities/TestRequestManager/types/request';

import {IRequestConfig} from 'server/utilities/HttpClient/IHttpClient';
import {isJson} from 'server/utilities/TestRequestManager/utilities/json';
import {parseContentType} from 'server/utilities/TestRequestManager/utilities/contentType';

const HIDDEN_HEADERS: string[] = [
    CommonHeaders.X_YA_SERVICE_TICKET,
    CommonHeaders.X_YA_USER_TICKET,
];

export function transformAxiosRequest(config: IRequestConfig): TRequest {
    const headers = config.headers ?? {};
    const contentType = parseContentType(headers);
    const baseRequest: IBaseRequest = {
        url: config.url ?? '',
        method: config.method ?? 'GET',
        params: normalizeUrlParams(config.params),
        headers: transformHeaders(headers),
    };

    if (contentType === 'application/json' && isJson(config.data)) {
        return {
            ...baseRequest,
            type: ERequestType.JSON,
            data: config.data,
        };
    }

    if (!config.data) {
        return {
            ...baseRequest,
            type: ERequestType.PLAIN,
        };
    }

    return {
        ...baseRequest,
        type: ERequestType.UNSUPPORTED,
    };
}

function normalizeUrlParams(params: unknown): Record<string, string> {
    const normalizedParams: Record<string, string> = {};

    if (params instanceof URLSearchParams) {
        params.forEach((value, key) => {
            normalizedParams[key] = value;
        });
    } else if (typeof params === 'object' && isPlainObject(params)) {
        forEach(params, (value, key) => {
            if (value !== null && value !== undefined) {
                normalizedParams[key] = String(value);
            }
        });
    }

    return normalizedParams;
}

function transformHeaders(headers: AxiosRequestHeaders): AxiosRequestHeaders {
    return mapValues(headers, (value, key) => {
        return HIDDEN_HEADERS.includes(key) ? '<redacted>' : value;
    });
}
