const TestpalmClient = require('@yandex-int/testpalm-api').default;
const {flatten} = require('ramda');
const fs = require('fs');
const path = require('path');
const {promisify} = require('util');
const {log} = require('../utils');

const fsReadFile = promisify(fs.readFile);
const fsWriteFile = promisify(fs.writeFile);

const REPORT_DIR_PATH = 'txt_reports';
const TESTPALM_IDS_REPORT_PATH = path.join(process.cwd(), `${REPORT_DIR_PATH}/testpalm_ids_report`);
const DIFFECTOR_PAGE_IDS_REPORT_PATH = path.join(process.cwd(), `${REPORT_DIR_PATH}/diffector_page_ids`);

const PROJECT_ID = 'marketmbi';
const MARKET_PAGE_ID_ATTRIBUTE = 'attributes.5c5d99ed70829809cfb46d5d';
const DEFAULT_EXPRESSION = {
    type: 'IN',
    key: MARKET_PAGE_ID_ATTRIBUTE,
    value: '',
};

const testpalmClient = new TestpalmClient(process.env.TESTPALM_OAUTH_API_TOKEN, {
    retryPostMethod: true,
    retryCount: 3,
});

// Минимальное кол-во страниц которое попадает под полный прогон от резолвера configs/diffector-resolvers/allPagesResolver
const MIN_PAGE_SIZE = 140;

/* istanbul ignore next */
const testPalmRequest = async filter => {
    try {
        const testcases = await testpalmClient.getTestCasesWithPost(PROJECT_ID, {
            include: ['id', 'attributes'],
            expression: filter,
        });

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
const getTestcases = async () => {
    const pagesList = await fsReadFile(DIFFECTOR_PAGE_IDS_REPORT_PATH, 'utf8');

    if (!pagesList) {
        return Promise.resolve([]);
    }

    const pages = pagesList.split(',');

    // В случае если затронуто большое количество страниц, то это значит что нужно делать полный прогон.
    // Как временное решение было решено сделать такую проверку на максимальное кол-во страниц для полного прогона.
    if (pages.length >= MIN_PAGE_SIZE) {
        // Если файл TESTPALM_IDS_REPORT_PATH - пустой, то произойдет полный прогон.
        return Promise.resolve([]);
    }

    const filter = {
        ...DEFAULT_EXPRESSION,
        value: pages,
    };
    const filterFormatted = JSON.stringify(filter);

    return testPalmRequest(filterFormatted);
};

getTestcases()
    .then(testcases => {
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
    })
    .catch(err => {
        console.error(err);
    });
