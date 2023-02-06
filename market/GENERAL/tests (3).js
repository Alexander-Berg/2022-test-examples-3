const fs = require('fs');
const path = require('path');

const TEST_ID_REGEXP = /marketmbi-\d+/g;

function parseIdsFromArgString(argsString, testIdRegexp = TEST_ID_REGEXP) {
    return argsString.match(testIdRegexp) || [];
}

function findTestFileInDir(pathname, id, fileRegExp) {
    const content = fs.readdirSync(pathname);

    for (let i = 0; i < content.length; i++) {
        const item = content[i];
        const pathToItem = path.join(pathname, content[i]);

        if (fs.statSync(pathToItem).isFile() && isMatchedFilename(item, fileRegExp)) {
            const code = fs.readFileSync(pathToItem, {encoding: 'utf-8'});
            if (isIdContainedInCode(id, code)) {
                return pathToItem;
            }
        } else if (fs.statSync(pathToItem).isDirectory()) {
            const filepath = findTestFileInDir(pathToItem, id, fileRegExp);

            if (filepath) {
                return filepath;
            }
        }
    }

    return null;
}

function isMatchedFilename(filename, fileRegExp) {
    return fileRegExp.test(filename);
}

function isIdContainedInCode(id, code) {
    return code.match(new RegExp(`id:\\s+('|"|\`)${id}('|\`|")`));
}

function createFilePathsArgs(argsString, ENTRY_POINT, FILENAME_REGEXP) {
    const testIds = parseIdsFromArgString(argsString);
    let actualArgsString = argsString;

    if (testIds.length > 0) {
        const filepaths = [];

        testIds.forEach(id => {
            const filepath = findTestFileInDir(ENTRY_POINT, id, FILENAME_REGEXP);

            if (filepath && !filepaths.includes(filepath)) {
                filepaths.push(filepath);
            }
        });

        actualArgsString = argsString.replace(
            `${testIds.join(' ')}`,
            `--runTestsByPath ${filepaths.join(' ')} -t '${testIds.join('|')}'`,
        );
    }

    return actualArgsString;
}

module.exports = {
    createFilePathsArgs,
    parseIdsFromArgString,
    findTestFileInDir,
    isMatchedFilename,
    isIdContainedInCode,
};
