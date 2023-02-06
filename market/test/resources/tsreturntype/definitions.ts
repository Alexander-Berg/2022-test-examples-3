/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class TsReturnTypeController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public request(): Promise<ComplexObject> {
    return this.fetchApi(`/api/json`, {
      method: RequestMethod.POST,
      responseType: ResponseType.JSON,
    });
  }
}

export interface ComplexObject {
  complexField: string[];
}
