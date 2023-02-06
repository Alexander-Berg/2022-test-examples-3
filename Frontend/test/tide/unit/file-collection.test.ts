import { assert } from 'chai';
import sinon from 'sinon';
import { FileCollection, TestFile } from '../../../src';

describe('tide / file-collection', () => {
    let fileCollection: FileCollection;
    const testFiles = [
        {
            tool: 'hermione',
            filePath: '/dir/file-1.hermione.js',
        },
        {
            tool: 'testpalm',
            filePath: '/dir/file-1.testpalm.yml',
        },
        {
            tool: 'hermione',
            filePath: '/dir/dir-2/file-3.hermione.js',
        },
        {
            tool: 'hermione',
            filePath: '/another-dir/file-4.hermione.js',
        },
    ] as TestFile[];

    beforeEach(() => {
        fileCollection = new FileCollection();
    });

    afterEach(() => {
        sinon.restore();
    });

    describe('mappings', () => {
        it('should save mappings correctly', () => {
            const mappings = [
                { from: 'file-1.hermione.js', tos: ['file-1.testpalm.yml'] },
                { from: 'file-2.txt', tos: ['another-file-1.txt', 'another-file-2.txt'] },
                { from: '/directory/file-3.ts', tos: ['/some/path/build/file-3.js'] },
            ];

            for (const { from, tos } of mappings) {
                for (const to of tos) {
                    fileCollection.addMapping(from, to);
                }
            }

            for (const { from, tos } of mappings) {
                assert.deepEqual(fileCollection.getMapping(from).sort(), tos.sort());
            }
        });

        it('should keep values unique', () => {
            fileCollection.addMapping('file-1', 'file-2');
            fileCollection.addMapping('file-1', 'file-2');

            assert.deepEqual(fileCollection.getMapping('file-1'), ['file-2']);
        });
    });

    describe('add/get testFiles', () => {
        it('should map file paths to test files correctly', () => {
            const testFile = {
                tool: 'hermione',
                filePath: '/dir/file-1.hermione.js',
            } as TestFile;

            fileCollection.addFile(testFile);

            const actualTestFile = fileCollection.getFile('/dir/file-1.hermione.js');

            assert.deepEqual(actualTestFile, testFile);
        });

        it('should map tools to test files correctly', () => {
            const expectedHermioneTestFiles = [
                {
                    tool: 'hermione',
                    filePath: '/dir/file-1.hermione.js',
                },
                {
                    tool: 'hermione',
                    filePath: '/dir/dir-2/file-3.hermione.js',
                },
                {
                    tool: 'hermione',
                    filePath: '/another-dir/file-4.hermione.js',
                },
            ] as TestFile[];
            const expectedTestpalmTestFiles = [
                {
                    tool: 'testpalm',
                    filePath: '/dir/file-1.testpalm.yml',
                },
            ] as TestFile[];

            for (const testFile of testFiles) {
                fileCollection.addFile(testFile);
            }

            const actualHermioneTestFiles = fileCollection.getFilesByTool('hermione');
            const actualTestpalmTestFiles = fileCollection.getFilesByTool('testpalm');

            assert.deepEqual(actualHermioneTestFiles, expectedHermioneTestFiles);
            assert.deepEqual(actualTestpalmTestFiles, expectedTestpalmTestFiles);
        });
    });

    describe('eachFile', () => {
        it('should run callback for each test file', () => {
            for (const testFile of testFiles) {
                fileCollection.addFile(testFile);
            }

            const callbackStub = sinon.stub();

            fileCollection.eachFile(callbackStub);

            assert(callbackStub.callCount === 4);
            testFiles.forEach((testFile, index) => {
                assert(callbackStub.getCall(index).calledWith(testFile));
            });
        });

        it('should run callback for each test file of certain tool', () => {
            for (const testFile of testFiles) {
                fileCollection.addFile(testFile);
            }

            const callbackStub = sinon.stub();

            fileCollection.eachFile('hermione', callbackStub);

            assert(callbackStub.callCount === 3);
            callbackStub.getCall(0).calledWith(testFiles[0]);
            callbackStub.getCall(1).calledWith(testFiles[2]);
            callbackStub.getCall(2).calledWith(testFiles[3]);
        });
    });
});
