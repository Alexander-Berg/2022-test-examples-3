'use strict';

const fs = require('fs');
const path = require('path');

const _ = require('lodash');

const {parse} = require('./options');

module.exports = function hermioneBrokenTestsReporter(hermione, options) {
    if (!options) {
        return;
    }

    const _options = parse({
        options: options,
        env: process.env,
        argv: process.argv
    });

    if (!_options.enabled || _options.enabled === 'false') {
        return;
    }

    const reportDir = path.resolve(_options.reportDir);
    if (!fs.existsSync(reportDir)) {
        fs.mkdirSync(reportDir);
    }
    const reportFilepath = path.resolve(reportDir, _options.reportFilename);

    if (_options.mode === 'filter') {
        const brokenTestTitles = fs.readFileSync(reportFilepath).toString()
            .split('\n')
            .filter(l => l.length);

        hermione
            .on(hermione.events.AFTER_TESTS_READ, (testCollection) => {
                testCollection.disableAll();

                for (const testTitle of brokenTestTitles) {
                    testCollection.enableTest(testTitle);
                }
            });

    } else {
        const initBrokenTestsCollect = () => {
            const brokenFileStream = fs.createWriteStream(reportFilepath, {
                flags: 'w+',
                autoClose: true,
            });

            hermione
                .on(hermione.events.TEST_FAIL, (test) => {
                    brokenFileStream.write(`${test.fullTitle()}\n`);
                });
        };

        // проверяем есть ли hermione.isWorker - значит это версия 1.x
        const hasIsWorkerMethod = Boolean(hermione && typeof hermione.isWorker === 'function');

        // если нет метода isWorker или если есть метод isWorker и это hermione main process
        const initPlugin = hasIsWorkerMethod === false || (hasIsWorkerMethod && hermione.isWorker() === false);

        if (initPlugin) {
            initBrokenTestsCollect();
        }
    }
};
