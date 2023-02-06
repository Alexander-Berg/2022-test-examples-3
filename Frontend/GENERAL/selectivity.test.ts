import { when, resetAllWhenMocks } from 'jest-when';
import * as selectivity from '@yandex-int/selectivity';

import * as validator from './validator';
import { calcSinceCommit, CONFIG_PATH } from './selectivity';

import * as utils from '../../../utils/getChangedFiles';

jest.mock('@yandex-int/frontend.ci.utils');

jest.mock('./validator');
const validatorStub = validator as jest.Mocked<typeof validator>;

jest.mock('@yandex-int/selectivity');
const selectivityStub = selectivity as jest.Mocked<typeof selectivity>;

jest.mock('../../../utils/getChangedFiles');
const utilsStub = utils as jest.Mocked<typeof utils>;

describe('frontend-ci:format-jobs-data:selectivity:selectivity', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        resetAllWhenMocks();
    });

    describe('.calcSinceCommit', () => {
        const DEFAULT_COMMIT = 'commit';
        const DEFAULT_OPTS = { cwd: '/default/cwd' };

        beforeEach(() => {
            validatorStub.isValidSelectivityResult.mockReturnValue(true);
            selectivityStub.calc.mockResolvedValue({});
            utilsStub.getChangedFiles.mockReturnValue([]);
        });

        test('should get changed files since commit', async() => {
            await calcSinceCommit('1234567', DEFAULT_OPTS);

            expect(utilsStub.getChangedFiles).toHaveBeenCalledTimes(1);
            expect(utilsStub.getChangedFiles).toHaveBeenCalledWith('1234567');
        });

        it('should calc selectivity based on changed files', async() => {
            utilsStub.getChangedFiles.mockReturnValue(['foo/bar', 'baz/quux']);

            await calcSinceCommit(DEFAULT_COMMIT, { cwd: '/cwd' });

            expect(selectivityStub.calc).toHaveBeenCalledTimes(1);
            expect(selectivityStub.calc).toHaveBeenCalledWith(['foo/bar', 'baz/quux'], CONFIG_PATH, { cwd: '/cwd' });
        });

        it('should calc selectivity based on changed files with repo name prefix', async() => {
            utilsStub.getChangedFiles.mockReturnValue(['repo/foo/bar', 'repo/baz/quux']);

            await calcSinceCommit(DEFAULT_COMMIT, { cwd: '/cwd/repo' });

            expect(selectivity.calc).toHaveBeenCalledWith(['foo/bar', 'baz/quux'], expect.anything(), expect.anything());
        });

        it('should validate selectivity result by schema', async() => {
            selectivityStub.calc.mockResolvedValue({ projects: { 'foo/bar': {} } });

            await calcSinceCommit(DEFAULT_COMMIT, DEFAULT_OPTS);

            expect(validatorStub.isValidSelectivityResult).toHaveBeenCalledTimes(1);
            expect(validatorStub.isValidSelectivityResult).toHaveBeenCalledWith({ projects: { 'foo/bar': {} } });
        });

        it('should throw in case of invalid selectivity result', () => {
            selectivityStub.calc.mockResolvedValue({ projects: { 'something/invalid': {} } });

            validatorStub.isValidSelectivityResult.mockReturnValue(false);

            when(validatorStub.getSchemaError)
                .calledWith({ projects: { 'something/invalid': {} } })
                .mockReturnValue('schema error');

            return expect(calcSinceCommit(DEFAULT_COMMIT, DEFAULT_OPTS)).rejects.toThrow(
                new Error('Invalid selectivity result schema:\nschema error')
            );
        });

        it('should return selectivity result', () => {
            selectivityStub.calc.mockResolvedValue({ projects: { 'foo/bar': {} } });

            return expect(calcSinceCommit(DEFAULT_COMMIT, DEFAULT_OPTS)).resolves.toEqual({ projects: { 'foo/bar': {} } });
        });

        it("should extend an empty selectivity result by 'projects' property", () => {
            selectivityStub.calc.mockResolvedValue({});

            return expect(calcSinceCommit(DEFAULT_COMMIT, DEFAULT_OPTS)).resolves.toEqual({ projects: {} });
        });
    });
});
