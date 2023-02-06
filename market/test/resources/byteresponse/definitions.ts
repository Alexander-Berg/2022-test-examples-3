/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class ByteResponseController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public responseEntityCall1(param: any): Promise<Blob> {
    return this.fetchApi(`/some/path1`, {
      method: RequestMethod.GET,
      query: {
        param,
      },
      responseType: ResponseType.BLOB,
    });
  }

  public responseEntityCall2(param: any): Promise<Blob> {
    return this.fetchApi(`/some/path2`, {
      method: RequestMethod.GET,
      query: {
        param,
      },
      responseType: ResponseType.BLOB,
    });
  }
}
