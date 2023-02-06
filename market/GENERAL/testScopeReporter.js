/* eslint-disable no-console */

const fs = require('fs');
const path = require('path');
const {groupPages, getPageId, getHtmlPageId} = require('./utils');
const {promisify} = require('util');
const {flatten} = require('ramda');

const fsWriteFile = promisify(fs.writeFile);

const REPORT_DIR_PATH = 'txt_reports';
const DIFFECTOR_PAGE_IDS_REPORT_PATH = path.join(process.cwd(), `${REPORT_DIR_PATH}/diffector_page_ids`);

const testScopeReporter = async ({result}) => {
    const {client = [], html = []} = groupPages(Object.entries(result));

    if (!fs.existsSync(REPORT_DIR_PATH)) {
        fs.mkdirSync(REPORT_DIR_PATH);
    }

    const pageIds = flatten(
        client
            .map(([page]) => [...getPageId(page)])
            .concat(html.map(([page]) => [...getHtmlPageId(page)]))
            .filter(Boolean),
    );

    if (!pageIds.length) {
        await fsWriteFile(DIFFECTOR_PAGE_IDS_REPORT_PATH, '').then(() => {
            console.log('No one Changed Page');
        });
        return;
    }

    const pageIdsStr = [...new Set(pageIds)].join(',');

    await fsWriteFile(DIFFECTOR_PAGE_IDS_REPORT_PATH, pageIdsStr)
        .then(() => {
            console.log(`Changed Page IDS successfully written to file ${DIFFECTOR_PAGE_IDS_REPORT_PATH}`);
        })
        .catch(e => {
            console.log(`Error writing file ${DIFFECTOR_PAGE_IDS_REPORT_PATH}`, e);
        });
};

module.exports = testScopeReporter;
