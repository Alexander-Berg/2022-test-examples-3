#!/usr/bin/env node
'use strict';

const _ = require('lodash');
const path = require('path');
const fg = require('fast-glob');
const Hermione = require('hermione');

const { parseArgv, getTestFilePathes, getTestCollection } = require('./utils');
const {
    readYmlFile,
    updateYmlFile,
    updateTestFiles,
    updateAssetFiles,
    updateMetricsFiles
} = require('./file-fixer');

const options = parseArgv(process.argv);

console.log(`Searching:\n- ${options.path.join('\n- ')}\n`);

fg(options.path, { absolute: false, onlyFiles: true, baseNameMatch: true })
    .then(async ymlFilePathes => {
        if (ymlFilePathes.length === 0) {
            console.log('Not found yml files');
            return;
        }

        if (options.verbose) {
            console.log('Found yml files:');
            console.log(`- ${ymlFilePathes.join('\n- ')}`);
        }

        console.log(`Found ${ymlFilePathes.length} yml files`);

        if (options.rewrite) {
            console.log('\nRewriting files...');
        }

        const hermione = new Hermione('tools/hermione-testpalm-fixer/hermione.conf.js');
        const hermioneE2E = new Hermione('tools/hermione-testpalm-fixer/hermione-e2e.conf.js');

        return _.chunk(ymlFilePathes, options.chunkSize)
            .reduce((promise, ymlFilePathes) => {
                return promise.then(() => Promise.all(ymlFilePathes.map(async ymlFilePath => {
                    const data = await readYmlFile(ymlFilePath);
                    const allTestFilePathes = await getTestFilePathes(ymlFilePath, data);

                    const [testFilePathes, testFilePathesE2E] = allTestFilePathes
                        .reduce((acc, path) => {
                            acc[path.endsWith('hermione.e2e.js') ? 1 : 0].push(path);

                            return acc;
                        }, [[], []]);
                    
                    const testCollection = await getTestCollection(hermione, testFilePathes);
                    const testCollectionE2E = await getTestCollection(hermioneE2E, testFilePathesE2E);

                    const [replacedData] = await Promise.all([
                        updateYmlFile(ymlFilePath, data, options),
                        updateTestFiles(testFilePathes, options),
                        updateTestFiles(testFilePathesE2E, options)
                    ]);

                    allTestFilePathes.forEach(filePath => {
                        delete require.cache[require.resolve(path.relative(__dirname, filePath))];
                    });

                    const replacedTestCollection = await getTestCollection(hermione, testFilePathes);
                    const replacedTestCollectionE2E = await getTestCollection(hermioneE2E, testFilePathesE2E);

                    await Promise.all([
                        updateAssetFiles(testFilePathes, testCollection, replacedTestCollection, options),
                        updateAssetFiles(testFilePathesE2E, testCollectionE2E, replacedTestCollectionE2E, options),
                        updateMetricsFiles(testFilePathes, testCollection, replacedTestCollection, options)
                    ]);
                })));
            }, Promise.resolve());
    })
    .then(() => {
        console.log('\nFinish');

        process.exit(0);
    })
    .catch(e => {
        console.error(e);
    });
