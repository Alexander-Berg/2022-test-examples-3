'use strict';

// Валидация тестовых сценариев

const assert = require('assert');
const chalk = require('chalk');

module.exports = (changedFiles, { npx, log }) => {
    const testFiles = changedFiles.rejectBy(/^\.config|^a\.yaml|\.tokens\.ya?ml$/).filterBy(/\.(ya?ml|hermione\.js)$/);

    if (testFiles.isEmpty) return;

    const invalidFiles = testFiles.filterBy(/\.ya?ml$/).filter(file => !file.endsWith('.testpalm.yml'));

    if (!invalidFiles.isEmpty) {
        log(chalk.red('Все тестовые сценарии должны оканчиваться на .testpalm.yml'));
        log(chalk.green(invalidFiles.asLines()));

        assert(false);
    }

    npx(`palmsync validate ${testFiles.asArgs()} --skip=tests`);
};
