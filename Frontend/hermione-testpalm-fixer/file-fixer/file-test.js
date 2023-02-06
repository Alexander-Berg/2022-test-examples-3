'use strict';

const fs = require('fs');
const util = require('util');
const j = require('jscodeshift');
const { CLIEngine } = require('eslint');

const { executeTransforms } = require('./../transforms');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const updateTestFiles = async (filePathes, options) => {
    await Promise.all(filePathes.map(async filePath => {
        try {
            const code = await readFile(filePath, 'utf-8');
            const ast = j(code);
            const replacedAst = executeTransforms('hermione', ast);

            let replacedCode = replacedAst.toSource({ quote: 'auto' });

            if (code !== replacedCode && options.rewrite) {
                try {
                    const linter = new CLIEngine({ fix: true });
                    const eslintResult = linter.executeOnText(replacedCode, filePath).results[0];

                    if (eslintResult.hasOwnProperty('output')) {
                        replacedCode = eslintResult.output;
                    }
                } catch (e) {
                    console.error(e);
                }

                await writeFile(filePath, replacedCode, 'utf-8');

                if (options.verbose) {
                    console.log(`- ${filePath}`);
                }
            }
        } catch (e) {
            console.log('Parsing error:', filePath);
            console.error(e);
        }
    }));
};

module.exports = {
    updateTestFiles
};
