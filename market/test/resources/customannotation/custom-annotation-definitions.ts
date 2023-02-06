/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class CustomController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public makeCall(param: number): Promise<Result<Output>> {
    return this.fetchApi(`/api/call`, {
      method: RequestMethod.GET,
      query: {
        param,
      },
      responseType: ResponseType.JSON,
    });
  }

  public makeVoid(param: number): Promise<Result<void>> {
    return this.fetchApi(`/api/void`, {
      method: RequestMethod.GET,
      query: {
        param,
      },
      responseType: ResponseType.JSON,
    });
  }
}

export interface Item {
  name?: string;
}

export interface Output {
  booleanData: boolean;
  booleanDataBox: boolean;
  byteData: number;
  byteDataBox: number;
  charData: string;
  charDataBox: string;
  intData: number;
  intDataBox: number;
  items: Item[];
  itemsByName: { [index: string]: Item };
  longData: number;
  longDataBox: number;
  shortData: number;
  shortDataBox: number;
  stringData: string;
  uniqueItems: Item[];
}

export interface Result<T> {
  data: T;
  done: boolean;
  message: string;
}
