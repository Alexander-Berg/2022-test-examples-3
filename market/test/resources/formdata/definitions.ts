/* tslint:disable */
/* eslint-disable */

import {FetchAPI, ResponseType, BodyType, RequestMethod, RequestOptions} from '../../../main/resources/common';

export class FormDataController {

  constructor(protected fetchApi: FetchAPI) {
  }

  public uploadFile(file: File): Promise<string> {
    return this.fetchApi(`/base-url/api/file/upload`, {
      method: RequestMethod.POST,
      body: {
        file,
      },
      bodyType: BodyType.FORM,
      responseType: ResponseType.TEXT,
    });
  }

  public uploadFiles(file1: File, file2: File): Promise<string> {
    return this.fetchApi(`/base-url/api/files/upload`, {
      method: RequestMethod.POST,
      body: {
        file1,
        file2,
      },
      bodyType: BodyType.FORM,
      responseType: ResponseType.TEXT,
    });
  }
}
