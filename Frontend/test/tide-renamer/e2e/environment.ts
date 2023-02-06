import path from 'path';
import sinon from 'sinon';
import inquirer from 'inquirer';
import mfs from 'mock-fs';

const backup = {};
const ESLint = require(path.resolve('node_modules/eslint')).ESLint;

function setupEnvironment(): void {
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

    // Настройка виртуальной файловой системы
    mfs(
        {
            ['.']: mfs.load(path.resolve('.')),
        },
        { createCwd: false },
    );
}

function restoreEnvironment(): void {
    sinon.restore();
    if (backup['process.exit']) {
        process.exit = backup['process.exit'];
    }
    if (backup['ESLint.prototype.lintText']) {
        ESLint.prototype.lintText = backup['ESLint.prototype.lintText'];
    }
    mfs.restore();
    delete require.cache[path.resolve('test/tide-renamer/fixtures/events.js')];
    delete require.cache[path.resolve('test/tide-renamer/fixtures/tide-spy-plugin.js')];
    delete require.cache[path.resolve('bin/tide.js')];
    delete require.cache[path.resolve('build/src/cli/index.js')];
    // Это нужно из-за того, что tide вешает кучу listener'ов на process, а это вызывает memory leak warning.
    process.removeAllListeners();
}

export { setupEnvironment, restoreEnvironment };
