import {TRequestSource} from 'server/utilities/TestRequestManager/types/requestSource';
import {TResponse} from 'server/utilities/TestRequestManager/types/response';
import {TRequest} from 'server/utilities/TestRequestManager/types/request';
import {EApiEntry} from 'types/EApiEntry';

export enum ERequestEventType {
    START_REQUEST = 'START_REQUEST',
    END_REQUEST = 'END_REQUEST',
}

export interface IStartRequestEvent {
    type: ERequestEventType.START_REQUEST;
    id: string;
    request: TRequest;
    apiHostType: EApiEntry | null;
    source: TRequestSource;
    timestamp: number;
}

export interface IEndRequestEvent {
    type: ERequestEventType.END_REQUEST;
    id: string;
    isAborted: boolean;
    response: TResponse | null;
    timestamp: number;
}

export type TRequestEvent = IStartRequestEvent | IEndRequestEvent;
