const mockedPaths: Record<string, unknown> = {};
// @ts-ignore
const originalNonWebpackRequire = global.__non_webpack_require__;

// @ts-ignore
global.__non_webpack_require__ = (path: string) => {
    if (path in mockedPaths) {
        return mockedPaths[path];
    }

    return originalNonWebpackRequire(path);
};

export const mockNonWebpackRequire = (path: string, value: unknown) => {
    mockedPaths[path] = value;
};
