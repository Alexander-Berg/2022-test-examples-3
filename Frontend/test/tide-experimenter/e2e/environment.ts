import path from 'path';
import sinon from 'sinon';
import inquirer from 'inquirer';
import mfs from 'mock-fs';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const backup: Record<string, any> = {};
const ESLint = require(path.resolve('node_modules/eslint')).ESLint;

export function setupEnvironment(): void {
    sinon.stub(inquirer, 'prompt').resolves({ continue: true });

    backup['process.exit'] = process.exit;

    // @ts-ignore
    process.exit = (): void => {};

    // Переопределение eslint нужно из-за того, что hermione-plugin/writer иначе не найдет конфиг
    backup['ESLint.prototype.lintText'] = ESLint.prototype.lintText;
    ESLint.prototype.lintText = function (text): string {
        return backup['ESLint.prototype.lintText'].call(this, text, {
            filePath: path.resolve('test/fixtures/test.js'),
        });
    };
}

export function restoreEnvironment(): void {
    sinon.restore();
    if (backup['process.exit']) {
        process.exit = backup['process.exit'];
    }
    if (backup['ESLint.prototype.lintText']) {
        ESLint.prototype.lintText = backup['ESLint.prototype.lintText'];
    }

    mfs.restore();
    delete require.cache[path.resolve('test/tide-experimenter/fixtures/events.js')];
    delete require.cache[path.resolve('test/tide-experimenter/fixtures/tide-spy-plugin.js')];
    delete require.cache[path.resolve('bin/tide.js')];
    delete require.cache[path.resolve('build/src/cli/index.js')];
    // Это нужно из-за того, что tide вешает кучу listener'ов на process, а это вызывает memory leak warning.
    process.removeAllListeners();
}

export function setupMockFs(dirToMount: string, inputDir: string): void {
    if (dirToMount.startsWith('src/')) {
        mfs(
            {
                ['.']: mfs.load(path.resolve('.')),
                [path.resolve('./src/features')]: mfs.load(path.resolve(inputDir, 'src/features')),
            },
            { createCwd: false },
        );
    } else if (dirToMount.startsWith('features/')) {
        mfs(
            {
                ['.']: mfs.load(path.resolve('.')),
                [path.resolve('./features')]: mfs.load(path.resolve(inputDir, 'features')),
            },
            { createCwd: false },
        );
    } else {
        mfs(
            {
                ['.']: mfs.load(path.resolve('.')),
            },
            { createCwd: false },
        );
    }
}
