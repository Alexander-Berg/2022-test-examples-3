import { Injectable } from '@nestjs/common';

@Injectable()
export class SingletonScopeService {
    // constructor() {
    //     console.log('SingletonScopeService::constructor');
    // }

    getData() {
        return Math.random().toString();
    }
}
