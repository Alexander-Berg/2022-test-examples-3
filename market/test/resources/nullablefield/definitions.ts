/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class NullableFieldController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public makeCall(input?: Input): Promise<void> {
    return this.fetchApi(`/api/call`, {
      method: RequestMethod.GET,
      query: {
        ...input,
      },
      responseType: ResponseType.VOID,
    });
  }
}

export interface Input {
  field?: string;
  getter?: string;
  setterIsNotEnough: string;
}
