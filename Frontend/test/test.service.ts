import { Injectable } from '@nestjs/common';

import { AsyncStorageService } from '../asyncStorage';

@Injectable()
export class TestService {
    key = Symbol();

    constructor(private asyncStorageService: AsyncStorageService) {}

    getData() {
        if (this.asyncStorageService.getData(this.key) === undefined) {
            this.asyncStorageService.setData(
                this.key,
                Math.random().toString(),
            );
        }

        return this.asyncStorageService.getData(this.key);
    }
}
