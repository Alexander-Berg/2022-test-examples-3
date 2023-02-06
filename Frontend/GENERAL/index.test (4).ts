import * as utils from './utils';
import { formatJobsData } from '.';
import { LargeProjects } from './constants';

import { TSelectiveCheckData } from '../types';
import { Check, Platform } from '../constants';

jest.mock('./utils');
const utilsStub = utils as jest.Mocked<typeof utils>;

describe('frontend-ci:format-jobs-data:formatter:index', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('.formatJobsData', () => {
        const DEFAULT_OPTS = { cwd: 'default/cwd' };

        beforeEach(() => {
            utilsStub.getPackageName.mockReturnValue('');
        });

        test('should format default data in case of no checks', () => {
            const input = {
                projects: {
                    'foo/bar': {
                        checks: {},
                    },
                },
            };
            const output = {
                [Check.E2e]: { items: [] },
                [Check.Unit]: { items: [] },
                [Check.Drone]: { items: [] },
                [Check.Deploy]: { items: [] },
                [Check.Hermione]: { items: [] },
                [Check.HermioneCoverage]: { items: [] },
                [Check.PulseStatic]: { items: [] },
                [Check.CheckTemplates]: { items: [] },
                [Check.ExpflagsUpload]: { items: [] },
                [Check.ExpflagsConvertToTestids]: { items: [] },
                [Check.TestpalmValidate]: { items: [] },
                [Check.TestpalmSynchronize]: { items: [] },
                [Check.PulseShooter]: { items: [] },
                [Check.PulseShooterTouch]: { items: [] },
                [Check.PulseShooterDesktop]: { items: [] },
                [Check.PulseBuild]: { items: [] },
            };

            expect(formatJobsData(input, DEFAULT_OPTS)).toEqual(output);
        });

        test('should format project data for check', () => {
            utilsStub.getPackageName.mockImplementation(absLocation => `${absLocation}-name`);

            const input = {
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Unit]: [''],
                        },
                    },
                },
            };
            const output = {
                [Check.Unit]: {
                    items: [
                        { project: { name: '/cwd/foo/bar-name', location: 'foo/bar' } },
                    ],
                },
            };

            expect(formatJobsData(input, { cwd: '/cwd' })).toMatchObject(output);
        });

        test(`should add "isLarge" property with falsy value for "${Check.Hermione}" check`, () => {
            const input = {
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Hermione]: [''],
                        },
                    },
                },
            };
            const output = {
                [Check.Hermione]: {
                    items: [
                        { isLarge: false },
                    ],
                },
            };

            expect(formatJobsData(input, DEFAULT_OPTS)).toMatchObject(output);
        });

        test(`should add "isLarge" property with truthy value for "${Check.Hermione}" check`, () => {
            utilsStub.getPackageName.mockImplementation(() => Array.from(LargeProjects)[0]);

            const input = {
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.Hermione]: [''],
                        },
                    },
                },
            };
            const output = {
                [Check.Hermione]: {
                    items: [
                        { isLarge: true },
                    ],
                },
            };

            expect(formatJobsData(input, DEFAULT_OPTS)).toMatchObject(output);
        });

        test('should add "platforms" property for check', () => {
            const input = {
                projects: {
                    'foo/bar': {
                        checks: {
                            [Check.E2e]: {
                                platforms: {
                                    [Platform.Desktop]: [''],
                                    [Platform.Touch]: [''],
                                },
                            } as TSelectiveCheckData,
                        },
                    },
                },
            };
            const output = {
                [Check.E2e]: {
                    items: [
                        { platforms: [Platform.Desktop, Platform.Touch] },
                    ],
                },
            };

            expect(formatJobsData(input, DEFAULT_OPTS)).toMatchObject(output);
        });
    });
});
