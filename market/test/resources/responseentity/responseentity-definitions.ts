/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class ResponseEntityController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public responseEntityCall1(): Promise<Result> {
    return this.fetchApi(`/some/path1`, {
      method: RequestMethod.GET,
      responseType: ResponseType.JSON,
    });
  }

  public responseEntityCall2(): Promise<any> {
    return this.fetchApi(`/some/path2`, {
      method: RequestMethod.GET,
      responseType: ResponseType.JSON,
    });
  }
}

export interface Result {
  data: string;
}
