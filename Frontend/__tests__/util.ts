export function assertDefinedAndReturn<T>(value: T | null | undefined, msg?: string): T {
    if (typeof value === 'undefined' || value === null) {
        throw new Error(msg || 'Not defiend');
    }

    return value;
}
