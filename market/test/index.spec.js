/* eslint-disable prefer-template */

const Fs = require('fs');
const Path = require('path');
const ChildProcess = require('child_process');

const tap = require('tap');

const testRules = require('../lib/constraints/test-rules');
const testCases = require('./lib/test-cases');
const paths = require('./lib/paths');
const build = require('./lib/build');

const updateSnapshots = process.env.UPDATE_SNAPSHOTS;

testCases().forEach(([name, environment, config]) => tap.test(`${environment}: ${name}`, {buffered: true}, tap => {
    build(config, environment);

    if (updateSnapshots) {
        prepareSnapshotsDirectory(environment, name);
    }

    const nameFixer = fixSnapshotName.bind(null, environment);

    const distPath = Path.resolve(paths.results, environment, name);
    const snapPath = Path.resolve(paths.snapshots, environment, name);
    const files = Fs.readdirSync(distPath);

    if (!updateSnapshots) {
        const snapshots = Fs.readdirSync(snapPath).sort();
        const fixedNames = files.map(nameFixer).sort();

        tap.same(fixedNames, snapshots, `Bundles list check for ${environment}: ${name}`);
    }

    for (const file of files) {
        tap.test(file, tap => {
            const checker = testRules.images.test(file)
                ? checkBinaryFile
                : checkTextFile;

            checker(tap, environment, name, distPath, snapPath, file, nameFixer);

            tap.end();
        });
    }

    tap.end();
}));

function checkTextFile(tap, environment, name, distPath, snapPath, file, nameFixer) {
    const contents = Fs.readFileSync(Path.resolve(distPath, file), 'utf8')
        .split(paths.filesRoot).join('')
        .replace(/\.[a-z0-9]{20}\.(css|js)/g, '.[some-hash].$1')
        .replace(/\.css\?.{4}/g, '.css')
        .replace(/\/\*! exports provided.*/g, '/*! exports provided: [unsorted exports] */')
        .replace(/\/\/# sourceMappingURL=.*/, '//# sourceMappingURL=[source map goes here]');

    const snapFile = Path.resolve(snapPath, nameFixer(file));

    if (updateSnapshots) {
        Fs.writeFileSync(snapFile, contents);
    } else {
        const snapshot = Fs.readFileSync(snapFile, 'utf8');
        tap.isEqual(contents, snapshot, `Snapshot check for ${environment}: ${name} > ${snapFile}`);
    }
}

function checkBinaryFile(tap, environment, name, distPath, snapPath, file, nameFixer) {
    const contents = Fs.readFileSync(Path.resolve(distPath, file));
    const snapFile = Path.resolve(snapPath, nameFixer(file));

    if (updateSnapshots) {
        Fs.writeFileSync(snapFile, contents);
    } else {
        const snapshot = Fs.readFileSync(snapFile);
        tap.isEqual(contents.compare(snapshot), 0, `Snapshot check for ${environment}: ${name} > ${snapFile}`);
    }
}

function fixSnapshotName(environment, name) {
    if (environment !== 'production' || /manifest/.test(name) || testRules.images.test(name)) {
        return name;
    }

    const chunks = name.split('.');
    chunks.splice(-2, 1);
    return chunks.join('.');
}

function prepareSnapshotsDirectory(environment, name) {
    try {
        Fs.lstatSync(Path.resolve(paths.snapshots, environment));
    } catch (e) {
        Fs.mkdirSync(Path.resolve(paths.snapshots, environment));
    }

    try {
        Fs.lstatSync(Path.resolve(paths.snapshots, environment, name));
        ChildProcess.execSync('rm -rf ' + Path.resolve(paths.snapshots, environment, name));
    } catch (e) { /* empty */ }

    Fs.mkdirSync(Path.resolve(paths.snapshots, environment, name));
}
