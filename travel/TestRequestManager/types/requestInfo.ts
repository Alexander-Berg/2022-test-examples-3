import {TRequest} from 'server/utilities/TestRequestManager/types/request';
import {TResponse} from 'server/utilities/TestRequestManager/types/response';
import {EApiEntry} from 'types/EApiEntry';
import {TRequestSource} from 'server/utilities/TestRequestManager/types/requestSource';

export interface IApiRequestInfo {
    id: string;
    request: TRequest;
    response: TResponse | null;
    isAborted: boolean;
    apiHostType: EApiEntry | null;
    source: TRequestSource;
    startTime: number;
    endTime: number | null;
    localStartTime?: number;
}
