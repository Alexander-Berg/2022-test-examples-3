/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class LocalDateInUriController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public localDateAsPathVariable(date: string): Promise<void> {
    return this.fetchApi(`/api/localDateAsPathVariable/${date}`, {
      method: RequestMethod.GET,
      responseType: ResponseType.VOID,
    });
  }

  public localDateAsRequestParam(date: string): Promise<void> {
    return this.fetchApi(`/api/localDateAsRequestParam`, {
      method: RequestMethod.GET,
      query: {
        date,
      },
      responseType: ResponseType.VOID,
    });
  }

  public localDateTimeAsPathVariable(dateTime: string): Promise<void> {
    return this.fetchApi(`/api/localDateTimeAsPathVariable/${dateTime}`, {
      method: RequestMethod.GET,
      responseType: ResponseType.VOID,
    });
  }

  public localDateTimeAsRequestParam(dateTime: string): Promise<void> {
    return this.fetchApi(`/api/localDateTimeAsRequestParam`, {
      method: RequestMethod.GET,
      query: {
        dateTime,
      },
      responseType: ResponseType.VOID,
    });
  }
}
