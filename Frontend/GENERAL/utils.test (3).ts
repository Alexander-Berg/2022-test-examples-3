import { rejectPackagesFromDirs } from './utils';

import { IChangedPackage } from '../../../utils/getChangedPackages';

describe('frontend-ci:format-jobs-data:selectivity:utils', () => {
    describe('.rejectPackagesFromDirs', () => {
        test('should reject packages from passed dirs', () => {
            const packages = [
                { location: '/cwd/foo/bar' } as IChangedPackage,
                { location: '/cwd/baz/quux' } as IChangedPackage,
                { location: '/cwd/asd/zxc' } as IChangedPackage,
            ];
            const dirs = ['/cwd/foo/bar', '/cwd/baz/quux'];

            expect(rejectPackagesFromDirs(packages, dirs)).toEqual([{ location: '/cwd/asd/zxc' }]);
        });
    });
});
