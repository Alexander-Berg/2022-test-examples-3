import path from 'path';
import { promises as fs } from 'fs';
import mfs from 'mock-fs';
import { assert } from 'chai';
import _ from 'lodash';
import sinon from 'sinon';
import workerpool, { WorkerPool } from 'workerpool';
import FilePathReader from '../../../src/file-path-reader';
import { mkTide } from '../utils';

describe('tide', () => {
    beforeEach(() => {
        mfs({
            [path.resolve('node_modules')]: mfs.load(path.resolve('node_modules')),
            [path.resolve('src/')]: mfs.load(path.resolve('src/')),
            'fixtures/': mfs.load(path.resolve('test/tide/fixtures/')),
        });
    });

    afterEach(() => {
        mfs.restore();
    });

    it('should read and parse all matching files', async () => {
        const tide = mkTide();
        const e = tide.events;
        const c = tide.constants;

        tide.on(e.BEFORE_FILES_READ, () => {
            tide.setFilePaths(c.hermione?.TOOL as string, ['fixtures/**/*.hermione.js']);
            tide.setFilePaths(c.testpalm?.TOOL as string, ['fixtures/**/*.testpalm.yml']);
        });

        tide.on(e.AFTER_FILES_READ, () => {
            tide.setFilePaths(c.hermione?.TOOL as string, []);
            tide.setFilePaths(c.testpalm?.TOOL as string, []);

            assert.deepEqual(
                _.map(
                    tide.fileCollection.getFilesByTool(c.hermione?.TOOL as string),
                    'filePath',
                ).sort(),
                [
                    'fixtures/dummy-project/file-2.hermione.js',
                    'fixtures/dummy-project/dir-2/file-2.hermione.js',
                    'fixtures/dummy-project/dir-1/nested/some.hermione.js',
                ].sort(),
            );

            assert.deepEqual(
                _.map(
                    tide.fileCollection.getFilesByTool(c.testpalm?.TOOL as string),
                    'filePath',
                ).sort(),
                [
                    'fixtures/dummy-project/dir-1/another.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/deeply-nested/file.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/some.testpalm.yml',
                    'fixtures/dummy-project/dir-2/file-1.testpalm.yml',
                    'fixtures/dummy-project/dir-2/file-2.testpalm.yml',
                    'fixtures/dummy-project/dir-3/file-1.testpalm.yml',
                    'fixtures/dummy-project/file-1.testpalm.yml',
                    'fixtures/dummy-project/file-2.testpalm.yml',
                ].sort(),
            );

            _.values(tide.parsers).forEach((parser) => parser.off());
        });

        await tide.run();
    });

    it('should ignore specified paths', async () => {
        const tide = mkTide({ ignore: ['**/dir-1/**', '**/dir-3/**'] });
        const e = tide.events;
        const c = tide.constants;

        tide.on(e.BEFORE_FILES_READ, () => {
            tide.setFilePaths(c.testpalm?.TOOL as string, ['fixtures/**/*.testpalm.yml']);
        });

        tide.on(e.AFTER_FILES_READ, () => {
            tide.setFilePaths(c.testpalm?.TOOL as string, []);

            assert.deepEqual(
                _.map(
                    tide.fileCollection.getFilesByTool(c.testpalm?.TOOL as string),
                    'filePath',
                ).sort(),
                [
                    'fixtures/dummy-project/dir-2/file-1.testpalm.yml',
                    'fixtures/dummy-project/dir-2/file-2.testpalm.yml',
                    'fixtures/dummy-project/file-1.testpalm.yml',
                    'fixtures/dummy-project/file-2.testpalm.yml',
                ].sort(),
            );

            _.values(tide.parsers).forEach((parser) => parser.off());
        });

        await tide.run();
    });

    it('should detect changes on fs when using cache', async () => {
        mfs({
            [path.resolve('node_modules')]: mfs.load(path.resolve('node_modules')),
            [path.resolve('src/')]: mfs.load(path.resolve('src/')),
            'fixtures/': mfs.load(path.resolve('test/tide/fixtures/')),
            'fixtures/cache/files.json': JSON.stringify({
                filePaths: [
                    'fixtures/dummy-project/dir-1/another.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/deeply-nested/file.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/some.testpalm.yml',
                    'fixtures/dummy-project/dir-2/file-1.testpalm.yml',
                    'fixtures/dummy-project/dir-2/file-2.testpalm.yml',
                    'fixtures/dummy-project/dir-3/file-1.testpalm.yml',
                    'fixtures/dummy-project/file-1.testpalm.yml',
                    'fixtures/dummy-project/file-2.testpalm.yml',
                ],
                patterns: ['fixtures/**/*.testpalm.yml'],
            }),
            'fixtures/cache/dirs.json': JSON.stringify({
                'fixtures/dummy-project': { mtime: 0 },
                'fixtures/dummy-project/dir-1/': { mtime: 10 },
                'fixtures/dummy-project/dir-1/nested/': { mtime: 20 },
                'fixtures/dummy-project/dir-1/nested/deeply-nested/': { mtime: 40 },
                'fixtures/dummy-project/dir-2/': { mtime: 50 },
                'fixtures/dummy-project/dir-3/': { mtime: 60 },
            }),
        });

        await fs.unlink('fixtures/dummy-project/dir-2/file-2.testpalm.yml');
        await fs.unlink('fixtures/dummy-project/file-2.testpalm.yml');
        await fs.writeFile(
            'fixtures/dummy-project/dir-1/nested/deeply-nested/new-file.testpalm.yml',
            '',
        );
        await fs.mkdir('fixtures/dummy-project/dir-3/new-dir');
        await fs.writeFile('fixtures/dummy-project/dir-3/new-dir/new-file.testpalm.yml', '');

        const mtimes = {
            'fixtures/dummy-project': { mtime: 0 },
            'fixtures/dummy-project/dir-1/': { mtime: 10 },
            'fixtures/dummy-project/dir-1/nested/': { mtime: 20 },
            'fixtures/dummy-project/dir-1/nested/deeply-nested/': { mtime: 46 },
            'fixtures/dummy-project/dir-2/': { mtime: 55 },
            'fixtures/dummy-project/dir-3/': { mtime: 65 },
            'fixtures/dummy-project/dir-1/another.testpalm.yml': {},
            'fixtures/dummy-project/dir-1/nested/deeply-nested/file.testpalm.yml': {},
            'fixtures/dummy-project/dir-1/nested/some.testpalm.yml': {},
            'fixtures/dummy-project/dir-2/file-1.testpalm.yml': {},
            'fixtures/dummy-project/dir-3/file-1.testpalm.yml': {},
            'fixtures/dummy-project/file-1.testpalm.yml': {},
        };
        sinon
            .stub(FilePathReader.prototype as any, '_getMTimes')
            .callsFake((paths) => _.pick(mtimes, paths));
        sinon.stub(workerpool, 'pool').returns({
            exec: () =>
                [
                    { 'fixtures/dummy-project/dir-3/new-dir': { mtime: 100 } },
                    ['fixtures/dummy-project/dir-3/new-dir/new-file.testpalm.yml'],
                ] as const,
        } as unknown as WorkerPool);

        let tide = mkTide();
        const e = tide.events;
        const c = tide.constants;

        tide.on(e.BEFORE_FILES_READ, () => {
            tide.setFilePaths(c.testpalm?.TOOL as string, ['fixtures/**/*.testpalm.yml']);
        });

        tide.on(e.AFTER_FILES_READ, () => {
            tide.setFilePaths(c.testpalm?.TOOL as string, []);

            assert.deepEqual(
                _.map(
                    tide.fileCollection.getFilesByTool(c.testpalm?.TOOL as string),
                    'filePath',
                ).sort(),
                [
                    'fixtures/dummy-project/dir-1/another.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/deeply-nested/file.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/some.testpalm.yml',
                    'fixtures/dummy-project/dir-2/file-1.testpalm.yml',
                    'fixtures/dummy-project/dir-3/file-1.testpalm.yml',
                    'fixtures/dummy-project/file-1.testpalm.yml',
                    'fixtures/dummy-project/dir-1/nested/deeply-nested/new-file.testpalm.yml',
                    'fixtures/dummy-project/dir-3/new-dir/new-file.testpalm.yml',
                ].sort(),
            );

            _.values(tide.parsers).forEach((parser) => parser.off());
        });

        await tide.run();
    });
});
