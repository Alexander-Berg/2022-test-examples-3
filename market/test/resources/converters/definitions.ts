/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class ConvertersController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public request(body: ComplexObject): Promise<string> {
    return this.fetchApi(`/api/json`, {
      method: RequestMethod.POST,
      query: {
        theBody: body,
      },
      responseType: ResponseType.TEXT,
    });
  }
}

export class StatusConverters {

  static apiStatusCode(value: Status): number {
    switch (value) {
      case Status.OPEN:
        return 1;
      case Status.CLOSED:
        return -1;
      case Status.NOT_AVAILABLE:
        return 404;
    }
  }

  static isAcceptItems(value: Status): boolean {
    switch (value) {
      case Status.OPEN:
        return true;
      case Status.CLOSED:
        return false;
      case Status.NOT_AVAILABLE:
        return false;
    }
  }

  static statusName(value: Status): string {
    switch (value) {
      case Status.OPEN:
        return 'Открыто';
      case Status.CLOSED:
        return 'Закрыто';
      case Status.NOT_AVAILABLE:
        return 'Не обслуживается';
    }
  }
}

export interface ComplexObject {
  complexField: Status[];
}

export enum Status {
  OPEN = 'OPEN',
  CLOSED = 'CLOSED',
  NOT_AVAILABLE = 'NOT_AVAILABLE',
}
