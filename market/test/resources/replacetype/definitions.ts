/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class ReplaceTypeController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public getIt(): Promise<SomeTooSmartType> {
    return this.fetchApi(`/api/getIt!`, {
      method: RequestMethod.GET,
      responseType: ResponseType.JSON,
    });
  }
}

export type SomeTooSmartType = string;
