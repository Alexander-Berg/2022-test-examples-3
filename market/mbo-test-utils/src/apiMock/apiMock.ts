import { SynchronousPromise } from 'synchronous-promise';

import {
  CallInfo,
  MockedApi,
  MockedApiApi,
  MockedApiObject,
  MockPromiseExtension,
  PromiseMock,
  RequestInfo,
} from './types';

class RequestMock<Response, Args extends any[]> {
  private callInfo: CallInfo<Response, Args>;
  private description: string;
  private extension: MockPromiseExtension | undefined;

  constructor(callInfo: CallInfo<Response, Args>, description: string, extension?: MockPromiseExtension) {
    this.callInfo = callInfo;
    this.extension = extension;
    this.description = description;
  }

  public args(): Args {
    return this.callInfo.args;
  }

  public resolve(response: Response) {
    if (this.extension && this.extension.beforeResolve) {
      this.extension.beforeResolve(this.description, this.callInfo.args);
    }
    this.callInfo.finalized = true;
    this.callInfo.resolve(response);
    if (this.extension && this.extension.afterResolve) {
      this.extension.afterResolve(this.description, this.callInfo.args);
    }
  }

  public reject(error: any) {
    if (this.extension && this.extension.beforeReject) {
      this.extension.beforeReject(this.description, this.callInfo.args);
    }
    this.callInfo.finalized = true;
    this.callInfo.reject(error);
    if (this.extension && this.extension.afterReject) {
      this.extension.afterReject(this.description, this.callInfo.args);
    }
  }
}

export function createPromiseMock<Response, Args extends any[]>(
  description: string,
  extension?: MockPromiseExtension
): PromiseMock<Response, Args> {
  const calls: Array<CallInfo<Response, Args>> = [];

  const mock = jest.fn<Promise<Response>, Args>((...args) => {
    let resolve: (value?: Response | PromiseLike<Response>) => void = null as any;
    let reject: (reason?: any) => void = null as any;
    const response = new SynchronousPromise<Response>((res, rej) => {
      resolve = res;
      reject = rej;
    });

    calls.push({
      args,
      reject,
      resolve,
      finalized: false,
    });

    return response;
  });

  const result: PromiseMock<Response, Args> = mock as any;
  result.next = filter => {
    const call = calls.find(c => !c.finalized && (!filter || filter(...c.args)));
    if (!call) {
      const callsString = calls
        .filter(c => !c.finalized)
        .map((c, i) => `#${i + 1}. ${JSON.stringify(c.args[0])}`)
        .join('\n\n');
      fail(`Can't find matching call, non-finalized calls:\n ${callsString}`);
    }

    return new RequestMock(call, description, extension);
  };
  result.activeRequests = () => calls.filter(c => !c.finalized).map(c => c.args);
  result.process = processor => {
    calls.filter(c => !c.finalized).forEach(c => processor(new RequestMock(c, '')));
  };

  return result;
}

export function createApiMock<T, Ext = Record<string, unknown>>(
  ext?: Ext,
  prefix?: string,
  extension?: MockPromiseExtension
): MockedApi<T> & T & Ext {
  const methods: Record<string, any> = {};
  const api: MockedApiApi = {
    activeRequests() {
      const requests: RequestInfo[] = [];
      Object.keys(methods).forEach(method => {
        const activeRequests = methods[method].activeRequests();
        if (activeRequests.length > 0) {
          requests.push({ name: method, requests: activeRequests });
        }
      });

      return requests;
    },
  };

  const proxy = new Proxy(
    {},
    {
      get(_target: Record<string, unknown>, key: string): any {
        if (key in api) {
          return api[key];
        }
        if (ext && key in ext) {
          return ext[key];
        }

        if (!methods[key]) {
          methods[key] = createPromiseMock(`${prefix}${key}`, extension);
        }

        return methods[key];
      },
    }
  );

  return proxy as any;
}

export function createApiObjectMock<T, Ext = Record<string, unknown>>(
  objectExtension?: Ext,
  extension?: MockPromiseExtension
): MockedApiObject<T & Ext> {
  const fields: Record<any, MockedApi<any>> = {};

  return new Proxy(
    {},
    {
      get(_target: Record<string, unknown>, p: string | number): any {
        if (objectExtension && p in objectExtension) {
          return (objectExtension as any)[p];
        }
        if (p === 'allActiveRequests') {
          const result = {} as Record<string, RequestInfo[]>;
          Object.entries(fields).forEach(([key, api]) => {
            const requests = api.activeRequests();
            if (requests.length > 0) {
              result[key] = requests;
            }
          });

          return result;
        }
        if (!fields[p]) {
          // Have to p.toString() to support symbol type to string conversion
          fields[p] = createApiMock<any>(undefined, `${p.toString()}.`, extension);
        }

        return fields[p];
      },
    }
  ) as any;
}
