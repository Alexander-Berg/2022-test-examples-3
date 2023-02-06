/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class NewController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public getPerson(name?: string): Promise<Person> {
    return this.fetchApi(`/v1/person/{name}`, {
      method: RequestMethod.GET,
      query: {
        ...name,
      },
      responseType: ResponseType.JSON,
    });
  }

  /**
   * @deprecated
   */
  public getPersonDeprecated(name?: string): Promise<Person> {
    return this.fetchApi(`/v0/person/{name}`, {
      method: RequestMethod.GET,
      query: {
        ...name,
      },
      responseType: ResponseType.JSON,
    });
  }

  public getPersons(names?: string[]): Promise<Person[]> {
    return this.fetchApi(`/v0/persons/{names}`, {
      method: RequestMethod.GET,
      query: {
        ...names,
      },
      responseType: ResponseType.JSON,
    });
  }
}

/**
 * @deprecated
 */
export class OldController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public getSmth(arg?: number): Promise<number> {
    return this.fetchApi(`/get_smth`, {
      method: RequestMethod.GET,
      query: {
        arg,
      },
      responseType: ResponseType.JSON,
    });
  }
}

export interface Person {
  fullName: string;
  /**
   * @deprecated
   */
  name: string;
}
