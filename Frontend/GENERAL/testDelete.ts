export function testDelete<T extends object>(obj: T, ...keys: (keyof T)[]) {
    for (const key of keys) {
        delete obj[key];
    }
}
