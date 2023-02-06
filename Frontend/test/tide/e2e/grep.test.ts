import path from 'path';
import mfs from 'mock-fs';
import { assert } from 'chai';
import _ from 'lodash';
import { mkTide } from '../utils';

describe('tide', () => {
    describe('grep', () => {
        let tide;
        let e;
        let c;

        beforeEach(() => {
            mfs(
                {
                    ['.']: mfs.load(path.resolve('.')),
                },
                { createCwd: false },
            );
        });

        afterEach(() => {
            mfs.restore();
        });

        it("should read all tests if grep wasn't provided", async () => {
            tide = mkTide();
            e = tide.events;
            c = tide.constants;

            tide.on(e.BEFORE_FILES_READ, () => {
                tide.parsers.hermione?.setOptions({ mode: 'ast' });
                tide.setFilePaths(c.hermione?.TOOL as string, [
                    'test/tide/fixtures/**/*.hermione.js',
                ]);
                tide.setFilePaths(c.testpalm?.TOOL as string, [
                    'test/tide/fixtures/**/*.testpalm.yml',
                ]);
            });

            tide.on(e.AFTER_FILES_READ, () => {
                tide.setFilePaths(c.hermione?.TOOL as string, []);
                tide.setFilePaths(c.testpalm?.TOOL as string, []);

                const testTitles = tide.testCollection.mapTests((t) => t.fullTitle());

                assert.deepEqual(
                    testTitles.sort(),
                    [
                        'Feature-one / Type-one Describe-1 The first it case',
                        'Feature-one / Type-one Describe-1 The second it case',
                        'Feature-one / Type-one Describe-2 The third it case',
                        'Feature-2 Describe one It-1',
                        'Feature-2 Describe one It-2',
                        'Feature-2 Describe-2 It-3',
                        'Feature nested Describe 1 It case 1',
                        'Feature nested Describe 2 It case 2',
                        'Feature nested Describe 2 It case 3',
                        'Feature nested Describe 3 It case 4',
                    ].sort(),
                );
                assert.deepEqual(
                    tide.fileCollection.files.map(_.property('filePath')).sort(),
                    [
                        'test/tide/fixtures/dummy-project/dir-1/another.testpalm.yml',
                        'test/tide/fixtures/dummy-project/dir-1/nested/deeply-nested/file.testpalm.yml',
                        'test/tide/fixtures/dummy-project/dir-1/nested/some.hermione.js',
                        'test/tide/fixtures/dummy-project/dir-1/nested/some.testpalm.yml',
                        'test/tide/fixtures/dummy-project/dir-2/file-1.testpalm.yml',
                        'test/tide/fixtures/dummy-project/dir-2/file-2.hermione.js',
                        'test/tide/fixtures/dummy-project/dir-2/file-2.testpalm.yml',
                        'test/tide/fixtures/dummy-project/dir-3/file-1.testpalm.yml',
                        'test/tide/fixtures/dummy-project/file-1.testpalm.yml',
                        'test/tide/fixtures/dummy-project/file-2.hermione.js',
                        'test/tide/fixtures/dummy-project/file-2.testpalm.yml',
                    ].sort(),
                );

                _.values(tide.parsers).forEach((parser) => parser.off());
            });

            await tide.run();
        });

        it('should filter out only matching tests', async () => {
            tide = mkTide({
                grep: /Describe one It-2|case \d+$/,
            });
            e = tide.events;
            c = tide.constants;

            tide.on(e.BEFORE_FILES_READ, () => {
                tide.parsers.hermione?.setOptions({ mode: 'ast' });
                tide.setFilePaths(c.hermione?.TOOL as string, [
                    'test/tide/fixtures/**/*.hermione.js',
                ]);
                tide.setFilePaths(c.testpalm?.TOOL as string, [
                    'test/tide/fixtures/**/*.testpalm.yml',
                ]);
            });

            tide.on(e.AFTER_FILES_READ, () => {
                tide.setFilePaths(c.hermione?.TOOL as string, []);
                tide.setFilePaths(c.testpalm?.TOOL as string, []);

                const testTitles = tide.testCollection.mapTests((t) => t.fullTitle());

                assert.deepEqual(
                    testTitles.sort(),
                    [
                        'Feature-2 Describe one It-2',
                        'Feature nested Describe 1 It case 1',
                        'Feature nested Describe 2 It case 2',
                        'Feature nested Describe 2 It case 3',
                        'Feature nested Describe 3 It case 4',
                    ].sort(),
                );
                assert.deepEqual(tide.fileCollection.files.map(_.property('filePath')).sort(), [
                    'test/tide/fixtures/dummy-project/dir-1/nested/some.hermione.js',
                    'test/tide/fixtures/dummy-project/dir-2/file-2.hermione.js',
                ]);

                _.values(tide.parsers).forEach((parser) => parser.off());
            });

            await tide.run();
        });
    });
});
