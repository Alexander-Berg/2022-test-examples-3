// eslint-disable-next-line max-classes-per-file
import {getBackendHandler, BackendResponse} from './backendHandlers';

export class MockBackend {
    static setup(name, backendParams) {
        return class {
            static backendName = name;

            name = name;

            // eslint-disable-next-line class-methods-use-this
            async prepareResponse(response) {
                return backendParams.fullResponse ? response : response?.body;
            }

            // eslint-disable-next-line class-methods-use-this
            async fetch(params) {
                const prepared = await this.prepareRequest(params);
                const response = await getBackendHandler()(name, prepared, backendParams);

                if (response instanceof BackendResponse) {
                    return this.prepareResponse(response, prepared);
                }

                return response;
            }

            static factory(stoutContext) {
                const instance = new this();
                instance.context = {stoutContext};
                instance.name = name;
                instance.prepareRequest = instance.prepareRequest.bind(instance);

                return instance;
            }

            async prepareRequest(params) {
                return {...params, backend: this.name};
            }
        };
    }

    static connect() {
        return class {
            // eslint-disable-next-line class-methods-use-this
            fetch(params) {
                return Promise.resolve(getBackendHandler()(null, params));
            }
        };
    }
}
