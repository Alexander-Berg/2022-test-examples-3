/* eslint-disable no-console */

const fs = require('fs');
const path = require('path');
const {promisify} = require('util');

const fsWriteFile = promisify(fs.writeFile);

const REPORT_DIR_PATH = 'txt_reports';
const DIFFECTOR_PAGES_REPORT_PATH = path.join(process.cwd(), `${REPORT_DIR_PATH}/diffector_pages`);
const ALIAS_MATCH_REGEXP = /\((.*)\)/;

const testScopeReporter = async ({result}) => {
    const pages = Object
        .entries(result)
        .map(([name]) => {
            // Выбираем страницы, которые указаны в aliases
            const matches = name.match(ALIAS_MATCH_REGEXP);

            if (matches) {
                return matches[1];
            }

            return undefined;
        })
        .filter(Boolean);

    if (!fs.existsSync(REPORT_DIR_PATH)) {
        fs.mkdirSync(REPORT_DIR_PATH);
    }

    if (!pages.length) {
        await fsWriteFile(DIFFECTOR_PAGES_REPORT_PATH, '').then(() => {
            console.log('No one Changed Page');
        });

        return;
    }

    const pagesStr = pages.join(',');

    await fsWriteFile(DIFFECTOR_PAGES_REPORT_PATH, pagesStr)
        .then(() => {
            console.log(`Changed pages successfully written to file ${DIFFECTOR_PAGES_REPORT_PATH}`);
        })
        .catch(error => {
            console.log(`Error writing file ${DIFFECTOR_PAGES_REPORT_PATH}`, error);
        });
};

module.exports = testScopeReporter;
