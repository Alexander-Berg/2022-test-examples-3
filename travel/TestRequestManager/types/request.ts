import {Method} from 'axios';

import {TJsonEntity} from 'server/utilities/TestRequestManager/types/json';

export enum ERequestType {
    JSON = 'JSON',
    PLAIN = 'PLAIN',
    UNSUPPORTED = 'UNSUPPORTED',
}

export interface IBaseRequest {
    url: string;
    method: Method;
    params: Record<string, string>;
    headers: Record<string, string>;
}

export interface IJsonRequest extends IBaseRequest {
    type: ERequestType.JSON;
    data: TJsonEntity;
}

export interface IPlainRequest extends IBaseRequest {
    type: ERequestType.PLAIN;
}

export interface IUnsupportedRequest extends IBaseRequest {
    type: ERequestType.UNSUPPORTED;
}

export type TRequest = IJsonRequest | IPlainRequest | IUnsupportedRequest;
