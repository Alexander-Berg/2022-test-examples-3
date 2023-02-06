/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class RequestBodyController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public requestJson(body: string): Promise<string> {
    return this.fetchApi(`/base-url/api/json`, {
      method: RequestMethod.POST,
      body: body,
      bodyType: BodyType.JSON,
      responseType: ResponseType.TEXT,
    });
  }
}
