/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class RequestUrlsController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public callWithNamedParam(param: number, namedParam: number): Promise<string> {
    return this.fetchApi(`/base-url/api`, {
      method: RequestMethod.GET,
      query: {
        param,
        named: namedParam,
      },
      responseType: ResponseType.TEXT,
    });
  }

  public callWithSinglePathVariable(id: number, name: string): Promise<string> {
    return this.fetchApi(`/base-url/api/${id}`, {
      method: RequestMethod.POST,
      query: {
        name,
      },
      responseType: ResponseType.TEXT,
    });
  }

  public callWithTwoPathVariables(a: number, bVariable: string): Promise<string> {
    return this.fetchApi(`/base-url/api/${a}/${bVariable}`, {
      method: RequestMethod.POST,
      responseType: ResponseType.TEXT,
    });
  }

  public simpleDeleteCall(): Promise<string> {
    return this.fetchApi(`/base-url/api/delete`, {
      method: RequestMethod.DELETE,
      responseType: ResponseType.TEXT,
    });
  }

  public simpleGetCall(): Promise<string> {
    return this.fetchApi(`/base-url/api/get`, {
      method: RequestMethod.GET,
      responseType: ResponseType.TEXT,
    });
  }

  public simplePatchCall(): Promise<string> {
    return this.fetchApi(`/base-url/api/patch`, {
      method: RequestMethod.PATCH,
      responseType: ResponseType.TEXT,
    });
  }

  public simplePostCall(): Promise<string> {
    return this.fetchApi(`/base-url/api/post`, {
      method: RequestMethod.POST,
      responseType: ResponseType.TEXT,
    });
  }

  public simplePutCall(): Promise<string> {
    return this.fetchApi(`/base-url/api/put`, {
      method: RequestMethod.PUT,
      responseType: ResponseType.TEXT,
    });
  }
}
