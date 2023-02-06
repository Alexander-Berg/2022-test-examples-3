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

const updateAssetFiles = async (filePathes, testCollection, replacedTestCollection, options) => {
    const originalTitles = getFullTitlesFromCollection(testCollection);
    const newTitles = getFullTitlesFromCollection(replacedTestCollection);

    return Promise.all(filePathes.map(async filePath => {
        const assetPathes = getAssetPathes(filePath, originalTitles, newTitles);

        return Promise.all(assetPathes.map(async assetPath => {
            try {
                let assetPathFrom;
                let isDefaultPathFrom = false;

                if (await exists(assetPath.from)) {
                    assetPathFrom = assetPath.from;
                } else if (assetPath.fromDefault && (await exists(assetPath.fromDefault))) {
                    assetPathFrom = assetPath.fromDefault;
                    isDefaultPathFrom = true;
                } else if (assetPath.fromDefault) {
                    console.warn(`- WARNING: Assets not found for ${filePath}`);
                    console.warn(`  - ${assetPath.titleFrom}`);
                    console.warn(`  - ${assetPath.from}`);
                }

                if (assetPathFrom && options.rewrite) {
                    if (options.soft || isDefaultPathFrom) {
                        await fse.copy(assetPathFrom, assetPath.to, { overwrite: true });
                    } else {
                        await fse.move(assetPathFrom, assetPath.to, { overwrite: true });
                    }

                    if (options.verbose) {
                        console.log(`- ${assetPath.to}`);
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

function getAssetPathes(filePath, originalTitles, newTitles) {
    if (!['hermione.js', 'hermione.e2e.js'].some(ext => filePath.endsWith(ext))) {
        return [];
    }

    const testDirName = path.dirname(filePath);

    return _.uniqWith(_.flatten(
        originalTitles
            .map((title, i) => {
                if (title !== newTitles[i]) {
                    return { from: title, to: newTitles[i] };
                }
            })
            .filter(Boolean)
            .map(titles => ([
                filePath.endsWith('hermione.js') ? {
                    titleFrom: titles.from,
                    titleTo: titles.to,
                    from: getAssetsFolderPath(testDirName, 'test-data', titles.from),
                    fromDefault: getDefaultCacheFolderPath(filePath, titles.from),
                    to: getAssetsFolderPath(testDirName, 'test-data', titles.to)
                } : null,
                {
                    from: getAssetsFolderPath(testDirName, 'screens', titles.from),
                    to: getAssetsFolderPath(testDirName, 'screens', titles.to)
                }
            ]))
    ).filter(Boolean), _.isEqual);
}

function getDefaultCacheFolderPath(filePath, fullTitle) {
    return path.join('hermione', 'test-data', getTestHash(filePath, fullTitle));
};

function getAssetsFolderPath(testDirName, partPath, fullTitle) {
    return path.join(testDirName, partPath, getFullTitleHash(fullTitle));
};

function getTestHash(filePath, fullTitle) {
    const relativePath = path.relative(process.cwd(), filePath);

    return getShortMD5(`${relativePath} ${fullTitle}`, 7);
}

function getFullTitleHash(fullTitle) {
    return getShortMD5(fullTitle, 7);
}

module.exports = {
    updateAssetFiles
};
