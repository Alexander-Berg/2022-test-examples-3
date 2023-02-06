import { when, resetAllWhenMocks } from 'jest-when';

import { calc } from '.';
import * as selectivity from './selectivity';
import * as checksFilter from './checks-filter';

import { Check } from '../constants';

import * as utils from '../../../utils/getChangedPackages';

jest.mock('./selectivity');
const selectivityStub = selectivity as jest.Mocked<typeof selectivity>;

jest.mock('./checks-filter');
const checksFilterStub = checksFilter as jest.Mocked<typeof checksFilter>;

jest.mock('../../../utils/getChangedPackages');
const utilsStub = utils as jest.Mocked<typeof utils>;

describe('frontend-ci:format-jobs-data:selectivity:index', () => {
    afterEach(() => {
        jest.clearAllMocks();
        resetAllWhenMocks();
    });

    describe('.calc', () => {
        const DEFAULT_COMMIT = 'commit';
        const DEFAULT_OPTS = { cwd: '/default/cwd' };

        beforeEach(() => {
            selectivityStub.calcSinceCommit.mockResolvedValue({ projects: {} });
            utilsStub.getChangedPackagesSinceCommit.mockReturnValue([]);
            checksFilterStub.isCompatibleCheck.mockReturnValue(true);
        });

        test('should calc selectivity since commit', async() => {
            await calc('1234567', { cwd: '/foo/bar' });

            expect(selectivityStub.calcSinceCommit).toHaveBeenCalledTimes(1);
            expect(selectivityStub.calcSinceCommit).toHaveBeenCalledWith('1234567', { cwd: '/foo/bar' });
        });

        test('should detect changed packages since commit', async() => {
            await calc('1234567', DEFAULT_OPTS);

            expect(utilsStub.getChangedPackagesSinceCommit).toHaveBeenCalledTimes(1);
            expect(utilsStub.getChangedPackagesSinceCommit).toHaveBeenCalledWith('1234567');
        });

        test('should return selectivity result', () => {
            selectivityStub.calcSinceCommit.mockResolvedValue({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                        }
                    }
                }
            });

            return expect(calc(DEFAULT_COMMIT, DEFAULT_OPTS)).resolves.toEqual({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                        }
                    }
                }
            });
        });

        test('should add packages changed by deps', () => {
            selectivityStub.calcSinceCommit.mockResolvedValue({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                        }
                    }
                }
            });
            utilsStub.getChangedPackagesSinceCommit.mockReturnValue([
                { location: '/cwd/foo/bar' } as utils.IChangedPackage,
                { location: '/cwd/baz/quux' } as utils.IChangedPackage,
            ]);

            return expect(calc(DEFAULT_COMMIT, { cwd: '/cwd' })).resolves.toEqual({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                        }
                    },
                    'baz/quux': {
                        checks: Object.values(Check).reduce<Record<string, string[]>>((res, item) => {
                            res[item] = [];
                            return res;
                        }, {}),
                    }
                }
            });
        });

        test('should filter checks by compatibility from projects', () => {
            selectivityStub.calcSinceCommit.mockResolvedValue({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                            [Check.Drone]: []
                        }
                    }
                }
            });

            when(checksFilterStub.isCompatibleCheck)
                .calledWith(Check.Drone, '/cwd/foo/bar').mockReturnValue(false)
                .calledWith(Check.Unit, '/cwd/foo/bar').mockReturnValue(true);

            return expect(calc(DEFAULT_COMMIT, { cwd: '/cwd' })).resolves.toEqual({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                        }
                    }
                }
            });
        });

        test('should filter checks by compatibility from projects which were changed by deps', () => {
            selectivityStub.calcSinceCommit.mockResolvedValue({ projects: {} });
            utilsStub.getChangedPackagesSinceCommit.mockReturnValue([
                { location: '/cwd/foo/bar' } as utils.IChangedPackage,
            ]);

            when(checksFilterStub.isCompatibleCheck)
                .mockReturnValue(false)
                .calledWith(Check.Unit, '/cwd/foo/bar').mockReturnValue(true);

            return expect(calc(DEFAULT_COMMIT, { cwd: '/cwd' })).resolves.toEqual({
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [],
                        }
                    },
                }
            });
        });
    });
});
