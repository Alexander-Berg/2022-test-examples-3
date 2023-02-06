const mockHandlers = {};
export type BackendHandler = (
    name: string,
    requestParams?: Record<string, any>,
    backendParams?: Record<string, any>,
) => any;

export type SetBackendHandler = (sk: string, handler: BackendHandler) => void;
export const setBackendHandler: SetBackendHandler = (sk, handler) => {
    // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
    mockHandlers[sk] = (name, params, backendParams) => {
        return new Promise(resolve => {
            setTimeout(() => {
                resolve(handler(name, params, backendParams));
            }, 100);
        });
    };
};

export type RemoveBackendHandler = (sk: string) => void;
export const removeBackendHandler: RemoveBackendHandler = sk => {
    // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
    mockHandlers[sk] = undefined;
};

const emptyHandler = () => Promise.resolve();
export type GetBackendHandler = (sk: string) => BackendHandler;
// @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
export const getBackendHandler: GetBackendHandler = sk => mockHandlers[sk] || emptyHandler;
