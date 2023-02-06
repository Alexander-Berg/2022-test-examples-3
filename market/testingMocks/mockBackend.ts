import {getBackendHandler} from './backendHandlers';

export class MockBackend {
    // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
    static setup(name, backendParams) {
        return class {
            sk = '';
            // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
            fetch(params) {
                return Promise.resolve(getBackendHandler(this.sk)(name, params, backendParams));
            }
        };
    }
}
