export const partialWrapper = <T>(p: Partial<T>) => p as T;
export const partialWrapperGetter = <T>() => (p: Partial<T>) => p as T;
