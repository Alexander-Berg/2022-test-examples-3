import { getPackageName } from './utils';

import * as requireWrapper from '../../../utils/require';

jest.mock('../../../utils/require');
const requireWrapperStub = requireWrapper as jest.Mocked<typeof requireWrapper>;

describe('frontend-ci:format-jobs-data:formatter:utils', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('.getPackageName', () => {
        const DEFAULT_LOCATION = '/default/location';

        beforeEach(() => {
            requireWrapperStub.requirePackageJson.mockResolvedValue({});
        });

        test("should require 'package.json' to detect package name", () => {
            getPackageName('/cwd/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/cwd/foo/bar');
        });

        test('should get package name', () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ name: 'baz/quux' });

            expect(getPackageName(DEFAULT_LOCATION)).toEqual('baz/quux');
        });
    });
});
