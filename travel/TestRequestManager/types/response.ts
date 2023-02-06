import {TJsonEntity} from 'server/utilities/TestRequestManager/types/json';

export enum EResponseType {
    JSON = 'JSON',
    TEXT = 'TEXT',
    UNSUPPORTED = 'UNSUPPORTED',
}

export interface IBaseResponse {
    headers: Record<string, string>;
    status: number;
}

export interface IJsonResponse extends IBaseResponse {
    type: EResponseType.JSON;
    data: TJsonEntity;
}

export interface ITextResponse extends IBaseResponse {
    type: EResponseType.TEXT;
    data: string;
}

export interface IUnsupportedResponse extends IBaseResponse {
    type: EResponseType.UNSUPPORTED;
}

export type TResponse = IJsonResponse | ITextResponse | IUnsupportedResponse;
