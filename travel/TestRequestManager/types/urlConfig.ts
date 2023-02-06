import {ERequestSourceType} from 'server/utilities/TestRequestManager/types/requestSource';

export interface IErrorResponse {
    status: number;
}

export interface IUrlConfig {
    url: string;
    sources: ERequestSourceType[];
    params?: Record<string, string>;
    timeout?: number;
    errorResponse?: IErrorResponse;
}
