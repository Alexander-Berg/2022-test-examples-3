const TestpalmClient = require('@yandex-int/testpalm-api').default;
const {flatten} = require('ramda');
const fs = require('fs');
const path = require('path');
const {promisify} = require('util');
const {log} = require('../utils');

const fsWriteFile = promisify(fs.writeFile);

const {FILTER_PRESETS} = require('./filterPresets');

const REPORT_DIR_PATH = 'txt_reports';
const TESTPALM_IDS_REPORT_PATH = path.join(process.cwd(), `${REPORT_DIR_PATH}/testpalm_ids_report`);
const PROJECT_ID = 'marketmbi';

const testpalmClient = new TestpalmClient(process.env.TESTPALM_OAUTH_API_TOKEN, {
    retryPostMethod: true,
    retryCount: 3,
});

/* istanbul ignore next */
const testPalmRequest = async testSuiteId => {
    try {
        const testcases = await testpalmClient.getTestCasesFromTestSuite(PROJECT_ID, testSuiteId, {include: ['id']});

        log(`Found ${testcases.length} testcases`);

        const testcasesId = testcases.map(testcase => testcase.id);

        return testcasesId;
    } catch (error) {
        //  TestPalm иногда возвращает 503 по непонятным причинам, даже когда все работает
        if (error.statusCode !== 503) {
            throw new Error(error);
        }

        return [];
    }
};

/* istanbul ignore next */
const processTestPalmResposne = testcases => {
    if (!fs.existsSync(REPORT_DIR_PATH)) {
        fs.mkdirSync(REPORT_DIR_PATH);
    }

    if (!testcases.length) {
        fsWriteFile(TESTPALM_IDS_REPORT_PATH, '').then(() => {
            log('No TestPalm cases found');
        });
        return;
    }

    const testcaseIds = flatten(testcases).join('|');
    const formatedTestcaseIds = `${PROJECT_ID}-(${testcaseIds})`;
    fsWriteFile(TESTPALM_IDS_REPORT_PATH, formatedTestcaseIds).then(() => {
        log(`TestPalm case IDS successfully written to file ${TESTPALM_IDS_REPORT_PATH}`);
    });
};

/* istanbul ignore next */
const getFilteredTestCases = async () => {
    const [rawFilter] = process.argv.slice(2);
    const filterType = rawFilter.toLowerCase();
    const testSuiteId = Object.keys(FILTER_PRESETS).includes(filterType) ? FILTER_PRESETS[filterType] : filterType;

    return testPalmRequest(testSuiteId);
};

getFilteredTestCases()
    .then(processTestPalmResposne)
    .catch(err => {
        console.error(err);
    });
