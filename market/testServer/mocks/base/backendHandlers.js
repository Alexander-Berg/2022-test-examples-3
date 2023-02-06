let currentHandler;

export class BackendResponse {
    constructor(response) {
        if (response) {
            Object.keys(response).forEach(key => {
                this[key] = response[key];
            });
        }
    }
}

export const setBackendHandler = (handlers, defaultBackendHandler) => {
    currentHandler = (name, params, backendParams) =>
        new Promise(resolve => {
            setTimeout(() => {
                const handler = handlers[name]?.[params?.logName] || handlers.resource?.[name];
                const defaultHandler = handlers.defaultHandler || defaultBackendHandler;
                resolve(handler ? handler(params) : defaultHandler(name, params, backendParams));
            }, 100);
        });
};

const emptyHandler = () => Promise.resolve();
export const getBackendHandler = () => currentHandler || emptyHandler;
