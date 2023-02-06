export type PromiseCall<Response, Args extends any[]> = (...args: Args) => Promise<Response>;

export type MockedApiCall<T> = T extends PromiseCall<infer Response, infer Args> ? PromiseMock<Response, Args> : T;

export type MockedApi<T> = MockedApiApi & { [P in keyof T]: MockedApiCall<T[P]> };

export interface IRequestMock<Response, Args extends any[]> {
  args(): Args;
  resolve(response: Response): void;
  reject(error: any): void;
}

export interface PromiseMock<Response, Args extends any[]> extends jest.Mock<Promise<Response>, Args> {
  next(filter?: (...args: Args) => boolean | undefined): IRequestMock<Response, Args>;
  process(processor: (request: IRequestMock<Response, Args>) => void): void;
  activeRequests(): Args[];
}

export type MockedApiObject<T> = { [P in keyof T]: MockedApi<T[P]> & T[P] } & {
  allActiveRequests: () => { [P in keyof T]?: RequestInfo[] };
};

export interface CallInfo<Response, Args extends any[]> {
  args: Args;
  finalized: boolean;
  resolve: (value: Response) => void;
  reject: (error: any) => void;
}

export interface RequestInfo {
  name: string;
  requests: any[];
}

export interface MockPromiseExtension {
  beforeResolve?: (description: string, args: any) => void;
  afterResolve?: (description: string, args: any) => void;
  beforeReject?: (description: string, args: any) => void;
  afterReject?: (description: string, args: any) => void;
}

export interface MockedApiApi {
  activeRequests(): RequestInfo[];
}

interface IConfigController {
  getConfig<T>(): Promise<T>;
}

export interface IConfigApi {
  configController: IConfigController;
}
