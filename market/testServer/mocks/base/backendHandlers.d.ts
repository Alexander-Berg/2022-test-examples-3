export type BackendHandler = (
    name: string,
    requestParams: Record<string, any> & {logName: string},
    backendParams?: Record<string, any>,
) => unknown;

type MethodParams<T> = {
    backend: string;
    body?: T;
    logName: string;
    method: string;
    pathname: string;
    retry?: number;
};

export type BackendMock<T, R> = (params: MethodParams<T>) => R;

type BackendMockDefinition = Record<string, BackendMock<unknown, unknown>>;

export type BackendMocks = Record<string, BackendMockDefinition> & {
    defaultHandler?: BackendHandler;
    resource?: BackendMockDefinition;
};

export type SetBackendHandler = (handler: BackendHandler) => void;
export const setBackendHandler: SetBackendHandler;

export type GetBackendHandler = () => BackendHandler;
export const getBackendHandler: GetBackendHandler;

export type BackendResponseParams = Record<string, unknown>;

export class BackendResponse {
    constructor(params: BackendResponseParams);
}
