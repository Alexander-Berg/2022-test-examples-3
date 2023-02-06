/* eslint-disable */
import { makeRestApiCaller, CallRestApiOptions, CallRestApi } from '../_helpers';

interface CallApiOptions extends CallRestApiOptions {
    attachment?: {
        field: string;
        filename: string;
        contentType: string;
        buffer: Buffer;
    };
    body?: object;
}

const callRestApi = makeRestApiCaller('public/v1');

export const callApi: CallRestApi = (method, route, options?: CallApiOptions) => {
    let res = callRestApi(method, route, options);

    if (options && options.attachment) {
        const { field, filename, contentType, buffer } = options.attachment;

        res = res.attach(field, buffer, { filename, contentType });
    }

    if (options && options.body) {
        res = res.send(options.body);
    }

    return res;
};
