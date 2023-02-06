/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class StringifyController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public request(body: ComplexObject, files: File[]): Promise<string> {
    return this.fetchApi(`/api/json`, {
      method: RequestMethod.POST,
      body: {
        theBody: JSON.stringify(body),
        files,
      },
      bodyType: BodyType.FORM,
      responseType: ResponseType.TEXT,
    });
  }
}

export interface ComplexObject {
  complexField: string[];
}
