import {AxiosResponse} from 'axios';

import {
    EResponseType,
    IBaseResponse,
    TResponse,
} from 'server/utilities/TestRequestManager/types/response';

import {isJson} from 'server/utilities/TestRequestManager/utilities/json';
import {parseContentType} from 'server/utilities/TestRequestManager/utilities/contentType';

export function transformAxiosResponse(response: AxiosResponse): TResponse {
    const {headers, status, data} = response;
    const contentType = parseContentType(headers);

    const baseResponse: IBaseResponse = {
        headers,
        status,
    };

    if (contentType === 'application/json' && isJson(data)) {
        return {
            ...baseResponse,
            type: EResponseType.JSON,
            data,
        };
    }

    if (contentType === 'text/plain' && typeof data === 'string') {
        return {
            ...baseResponse,
            type: EResponseType.TEXT,
            data,
        };
    }

    return {
        ...baseResponse,
        type: EResponseType.UNSUPPORTED,
    };
}
