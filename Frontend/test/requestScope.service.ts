import { Injectable, Scope } from '@nestjs/common';

@Injectable({ scope: Scope.REQUEST })
export class RequestScopeService {
    // constructor() {
    //     console.log('RequestScopeService::constructor');
    // }

    getData() {
        return Math.random().toString();
    }
}
