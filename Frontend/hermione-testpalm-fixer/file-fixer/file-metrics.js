'use strict';

const fs = require('fs');
const path = require('path');
const util = require('util');
const fse = require('fs-extra');
const _ = require('lodash');
const { getShortMD5 } = require('@yandex-int/short-md5');

const { getFullTitlesFromCollection } = require('./../utils');
const { YML_IGNORE_KEYS, YML_SPECS_TITLE_KEYS, YML_SPECS_TYPE_KEYS } = require('./../constants');

const exists = util.promisify(fs.exists);
const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const updateMetricsFiles = async (filePathes, testCollection, replacedTestCollection, options) => {
    return Promise.all(filePathes.map(async filePath => {
        const metricsPathes = filePathes.map(getMetricsFilePath).filter(Boolean);

        return Promise.all(metricsPathes.map(async metricsPath => {
            try {
                if (await exists(metricsPath)) {
                    const metrics = JSON.parse(await readFile(metricsPath, 'utf-8'));
                    const replacedMetrics = updateMetricsFile(metrics, testCollection, replacedTestCollection);

                    if (!_.isEqual(metrics, replacedMetrics) && options.rewrite) {
                        await writeFile(metricsPath, `${JSON.stringify(replacedMetrics, null, 4)}\n`, 'utf-8');

                        if (options.verbose) {
                            console.log(`- ${metricsPath}`);
                        }
                    }
                }
            } catch (e) {
                if (e.code !== 'EEXIST') {
                    console.error(e);
                }
            }
        }));
    }));
};

function getMetricsFilePath(testFileName) {
    const testDirName = path.dirname(testFileName);
    const ext = '.hermione.js';

    if (testFileName.endsWith(ext)) {
        return path.join(testDirName, path.basename(testFileName, ext) + '.metrics.json');
    }
}

function updateMetricsFile(metrics, testCollection, replacedTestCollection) {
    const originalTitles = getFullTitlesFromCollection(testCollection);
    const newTitles = getFullTitlesFromCollection(replacedTestCollection);
    const replacedMetrics = {};
        
    _.keys(metrics).forEach(key => {
        const keyIndex = _.indexOf(originalTitles, key);
        const newKeys = keyIndex > -1
            ? newTitles[keyIndex]
            : key;

        if (newKeys) {
            replacedMetrics[newKeys] = metrics[key];
        }
    });
    
    return _.isEmpty(replacedMetrics) ? metrics : replacedMetrics;
}

module.exports = {
    updateMetricsFiles
};
