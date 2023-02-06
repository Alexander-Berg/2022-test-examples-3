const {getHash, getAllPackageFilePaths} = require('.');

describe('Utils', () => {
    test('getHash', () => {
        expect(getHash().length).toBe(6);
    });

    test('getAllPackageFilePaths', () => {
        expect(getAllPackageFilePaths().sort()).toEqual(
            [
                './packages/b2b/package.json',
                './packages/codemods/package.json',
                './packages/core/package.json',
                './packages/docs/package.json',
                './packages/market/package.json',
            ].sort(),
        );
    });
});
