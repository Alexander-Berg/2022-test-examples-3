/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class ConvertArgsToObjectController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public someMethod({ a, b, c, d }: { a: number; b: number; c?: number; d?: number; }): Promise<string> {
    return this.fetchApi(`/some/path`, {
      method: RequestMethod.GET,
      query: {
        a,
        b,
        c,
        d,
      },
      responseType: ResponseType.TEXT,
    });
  }
}
