import {getBackendHandler} from './backendHandlers';

// @ts-expect-error(TS7031) найдено в рамках MARKETPARTNER-16237
export const unsafeResource = ({sk}, name, params) => Promise.resolve(getBackendHandler(sk)(name, params));
