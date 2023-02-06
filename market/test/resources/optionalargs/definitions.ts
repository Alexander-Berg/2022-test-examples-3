/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class OptionalArgsController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public optional(visible?: boolean): Promise<string> {
    return this.fetchApi(`/api/args/optional`, {
      method: RequestMethod.GET,
      query: {
        visible,
      },
      responseType: ResponseType.TEXT,
    });
  }

  public pathVariable(required: string, optional?: string): Promise<string> {
    return this.fetchApi(`/api/args/pathVariable/${required}/${optional}`, {
      method: RequestMethod.GET,
      responseType: ResponseType.TEXT,
    });
  }

  public requestBodyOptional(optional?: string): Promise<string> {
    return this.fetchApi(`/api/args/requestBody/optional`, {
      method: RequestMethod.POST,
      body: optional,
      bodyType: BodyType.JSON,
      responseType: ResponseType.TEXT,
    });
  }

  public requestBodyRequired(required: string): Promise<string> {
    return this.fetchApi(`/api/args/requestBody/required`, {
      method: RequestMethod.POST,
      body: required,
      bodyType: BodyType.JSON,
      responseType: ResponseType.TEXT,
    });
  }

  public requestParam(required: string, optional?: string): Promise<string> {
    return this.fetchApi(`/api/args/requestParam`, {
      method: RequestMethod.GET,
      query: {
        required,
        optional,
      },
      responseType: ResponseType.TEXT,
    });
  }

  public sorting(one: string, two: string, three?: string, four?: string, five?: string): Promise<string> {
    return this.fetchApi(`/api/args/sorting`, {
      method: RequestMethod.GET,
      query: {
        three,
        four,
        one,
        five,
        two,
      },
      responseType: ResponseType.TEXT,
    });
  }
}
