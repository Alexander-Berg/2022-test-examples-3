import * as utils from '@yandex-int/frontend.ci.utils';
import * as common from './common';

jest.mock('@yandex-int/si.ci.sandbox-shovel');
jest.mock('@yandex-int/frontend.ci.utils');

describe('frontend.ci.autopublish: common', function() {
    describe('computeHashMapForFiles', function() {
        const testPackageJson = {
            name: '@yandex-int/frontend.ci.autopublish',
            version: '0.13.1',
            description: 'Set of CI tools for frontend monorepo autopublishing.',
            main: 'build/index.js',
            files: [
                'build',
                'src',
            ],
            license: 'UNLICENSED',
        };
        const getFileContent = jest.fn();

        afterEach(() => jest.resetAllMocks());

        it('should compute hashmap with given fn', async() => {
            getFileContent
                .mockImplementationOnce(() => JSON.stringify(testPackageJson));
            const actual = await common.computeHashMapForFiles(['some-file'], getFileContent);

            expect(getFileContent).toHaveBeenLastCalledWith('some-file');
            expect(actual).toHaveProperty('some-file', '65cc71b0bf596ab8495c393d4cfda39e0e015d5b');
        });

        it('should write null if fn throws', async() => {
            getFileContent
                .mockImplementationOnce(() => { throw new Error('oops') })
                .mockImplementationOnce(() => JSON.stringify(testPackageJson));
            const actual = await common.computeHashMapForFiles(['some-file-0', 'some-file-1'], getFileContent);

            expect(getFileContent).toHaveBeenCalledWith('some-file-0');
            expect(getFileContent).toHaveBeenCalledWith('some-file-1');
            expect(actual).toHaveProperty('some-file-0', null);
            expect(actual).toHaveProperty('some-file-1', '65cc71b0bf596ab8495c393d4cfda39e0e015d5b');
        });
    });

    describe('getDiffForHashMaps', function() {
        const hm1 = { a: '1', b: '2' };
        const hm2 = { a: '3', b: '2' };

        it('should create diff for hashmaps', async() => {
            const actual = common.getDiffForHashMaps(hm1, hm2);

            expect(actual).toHaveProperty('a', { actual: '3', expected: '1' });
        });
    });

    describe('readFileContentAtCommit', function() {
        const arcShowContent = `[
  {
    "hash":"0fbd3a61e85a6348c2596332ffa5c8fb14db3d3d",
    "type":"blob",
    "content":"{\\n  \\"name\\": \\"fake-package\\",\\n  \\"version\\": \\"0.2.9\\"\\n}\\n"
  }
]`;
        const arcFileContent = JSON.parse(arcShowContent)[0].content;
        const gitShowContent = `{
  "name": "fake-package",
  "version": "0.0.0"
  }`;

        const isArcMock = jest.spyOn(common, 'isArc');

        afterEach(() => jest.resetAllMocks());

        it('should get file at commit from arc', async() => {
            isArcMock.mockReturnValue(true);
            // @ts-ignore
            (utils.exec as jest.MockedFunction<typeof utils.exec>).mockReturnValue(arcShowContent);

            const actual = common.readFileContentAtCommit('path/to', 'HEAD', 'some-file');

            expect(utils.exec).toHaveBeenCalledWith('arc show --json HEAD:path/to/some-file', utils.FAILURE_BEHAVIOUR.THROW);
            expect(actual).toBe(arcFileContent);
        });

        it('should get file at commit from git', async() => {
            isArcMock.mockReturnValue(false);
            // @ts-ignore
            (utils.exec as jest.MockedFunction<typeof utils.exec>).mockReturnValue(gitShowContent);

            const actual = common.readFileContentAtCommit('', 'HEAD', 'some-file');

            expect(utils.exec).toHaveBeenCalledWith('git show HEAD:some-file', utils.FAILURE_BEHAVIOUR.THROW);
            expect(actual).toBe(gitShowContent);
        });
    });
});
