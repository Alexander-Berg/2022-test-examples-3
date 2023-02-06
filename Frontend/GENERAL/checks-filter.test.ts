import * as fs from 'fs';

import { Script } from './constants';
import { isCompatibleCheck } from './checks-filter';

import { Check } from '../constants';

import * as requireWrapper from '../../../utils/require';
import { DRONE_CONFIG_FILENAME, EXPFLAGS_SYNC_CONFIG_FILE_PATH } from '../../../utils/constants';

jest.mock('fs');
const fsStub = fs as jest.Mocked<typeof fs>;

jest.mock('../../../utils/require');
const requireWrapperStub = requireWrapper as jest.Mocked<typeof requireWrapper>;

describe('frontend-ci:format-jobs-data:selectivity:checks-filter', () => {
    const DEFAULT_LOCATION = '/default/location';

    beforeEach(() => {
        requireWrapperStub.requirePackageJson.mockReturnValue({});
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe(`'${Check.E2e}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.E2e, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.E2e]: ' ' } });

            expect(isCompatibleCheck(Check.E2e, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.E2e, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.Unit}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.Unit, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.Unit]: ' ' } });

            expect(isCompatibleCheck(Check.Unit, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.Unit, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.Deploy}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.Deploy, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.Deploy]: ' ' } });

            expect(isCompatibleCheck(Check.Deploy, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.Deploy, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.Hermione}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.Hermione, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.Hermione]: ' ' } });

            expect(isCompatibleCheck(Check.Hermione, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.Hermione, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.PulseStatic}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.PulseStatic, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.PulseStatic]: ' ' } });

            expect(isCompatibleCheck(Check.PulseStatic, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.PulseStatic, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.PulseShooter}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.PulseShooter, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.PulseShooter]: ' ' } });

            expect(isCompatibleCheck(Check.PulseShooter, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.PulseShooter, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.PulseShooterDesktop}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.PulseShooterDesktop, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.PulseShooter]: ' ' } });

            expect(isCompatibleCheck(Check.PulseShooterDesktop, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.PulseShooterDesktop, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.TestpalmValidate}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.TestpalmValidate, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: { [Script.TestpalmValidate]: ' ' } });

            expect(isCompatibleCheck(Check.TestpalmValidate, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({ scripts: {} });

            expect(isCompatibleCheck(Check.TestpalmValidate, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.CheckTemplates}'`, () => {
        test("should require 'package.json' to retrieve compatibility data", () => {
            isCompatibleCheck(Check.CheckTemplates, '/foo/bar');

            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledTimes(1);
            expect(requireWrapperStub.requirePackageJson).toHaveBeenCalledWith('/foo/bar');
        });

        test("should return 'true' if check is compatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({
                scripts: {
                    [Script.Artifacts]: ' ',
                    [Script.Build]: ' ',
                },
            });

            expect(isCompatibleCheck(Check.CheckTemplates, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            requireWrapperStub.requirePackageJson.mockReturnValue({
                scripts: {
                    [Script.Artifacts]: ' ',
                },
            });

            expect(isCompatibleCheck(Check.CheckTemplates, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.Drone}'`, () => {
        test('should check static file on existance to retrieve compatibility data', () => {
            isCompatibleCheck(Check.Drone, '/foo/bar');

            expect(fsStub.existsSync).toHaveBeenCalledTimes(1);
            expect(fsStub.existsSync).toHaveBeenCalledWith(`/foo/bar/${DRONE_CONFIG_FILENAME}`);
        });

        test("should return 'true' if check is compatible with the project", () => {
            fsStub.existsSync.mockReturnValue(true);

            expect(isCompatibleCheck(Check.Drone, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            fsStub.existsSync.mockReturnValue(false);

            expect(isCompatibleCheck(Check.Drone, DEFAULT_LOCATION)).toBeFalsy();
        });
    });

    describe(`'${Check.ExpflagsUpload}'`, () => {
        test('should check static file on existance to retrieve compatibility data', () => {
            isCompatibleCheck(Check.ExpflagsUpload, '/foo/bar');

            expect(fsStub.existsSync).toHaveBeenCalledTimes(1);
            expect(fsStub.existsSync).toHaveBeenCalledWith(`/foo/bar/${EXPFLAGS_SYNC_CONFIG_FILE_PATH}`);
        });

        test("should return 'true' if check is compatible with the project", () => {
            fsStub.existsSync.mockReturnValue(true);

            expect(isCompatibleCheck(Check.ExpflagsUpload, DEFAULT_LOCATION)).toBeTruthy();
        });

        test("should return 'false' if check is incompatible with the project", () => {
            fsStub.existsSync.mockReturnValue(false);

            expect(isCompatibleCheck(Check.ExpflagsUpload, DEFAULT_LOCATION)).toBeFalsy();
        });
    });
});
