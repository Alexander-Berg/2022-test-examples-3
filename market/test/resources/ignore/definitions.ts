/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class IgnoreMethodsAndParams {

  constructor(protected fetchApi: FetchAPI) {
  }

  public goodMethod(): Promise<SomeType> {
    return this.fetchApi(`/some/goodMethod`, {
      method: RequestMethod.GET,
      responseType: ResponseType.JSON,
    });
  }

  public ignore(): Promise<IgnorePojo> {
    return this.fetchApi(`/some/ignorePojo`, {
      method: RequestMethod.GET,
      responseType: ResponseType.JSON,
    });
  }

  public ignore2(): Promise<IgnorePojo2> {
    return this.fetchApi(`/some/ignorePojo2`, {
      method: RequestMethod.GET,
      responseType: ResponseType.JSON,
    });
  }

  public methodWithoutTsParams(): Promise<string> {
    return this.fetchApi(`/some/methodWithoutTsParams`, {
      method: RequestMethod.GET,
      responseType: ResponseType.TEXT,
    });
  }
}

export interface IgnorePojo {
  str1: string;
}

export interface IgnorePojo2 {
  str1: string;
}

export interface SomeType {
  date: string;
}
