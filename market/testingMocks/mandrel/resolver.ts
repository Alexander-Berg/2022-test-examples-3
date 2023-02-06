export {unsafeResource} from '../mockResource';

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
export const createResolver = impl => impl;

export class ResolverError {}
