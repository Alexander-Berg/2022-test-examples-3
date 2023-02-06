const rp = require('request-promise');
const {TESTPALM_URL, VALID_PLATFORM_TYPES} = require('./constants');
const {log} = require('../utils');

const RELEASE_BRANCH_PREFIX = 'release/';

let skipSource;

try {
    // eslint-disable-next-line global-require
    skipSource = require('../../configs/hermione/tests-config/skipped');
} catch (err) {
    console.error(err);
    skipSource = {};
}

const platformType = String(process.env.PLATFORM_TYPE || VALID_PLATFORM_TYPES[0])
    .trim()
    .toLowerCase();

const currentBranch = process.env.CURRENT_GIT_BRANCH;
const isReleaseBranch =
    String(currentBranch).startsWith(RELEASE_BRANCH_PREFIX) ||
    Boolean(process.env['hermione-allure_testpalm_run_version']);

if (!VALID_PLATFORM_TYPES.includes(platformType)) {
    log(`\nunknown PLATFORM_TYPE value: ${platformType}\nonly (${VALID_PLATFORM_TYPES}) platforms are supported `);
    process.exit(1);
}

const getPlatformSkips = () => {
    if (!skipSource[platformType]) return [];

    const releaseIssues = new Set(String(process.env.AT_RELEASE_ISSUES || '').split(/\s+/));

    return skipSource[platformType].reduce((acc, {issue, reason, cases, isInRelease = false}) => {
        if (!issue) {
            process.stdout.write('Invalid skipped test config, "issue" is required\n');
            process.exit(1);
        }

        if (isReleaseBranch && (isInRelease === true || releaseIssues.has(issue))) {
            return acc;
        }

        cases.forEach(({id, fullName}) =>
            acc.push({
                id,
                issue,
                reason,
                case: fullName,
            }),
        );

        return acc;
    }, []);
};

const parseId = ({id}) => (id.match(/\d+/) || [])[0];

const getFilter = cases => {
    if (!cases || !cases.length) return '';

    const copy = cases.slice(0);
    const right = {type: 'EQ', key: 'id', value: parseId(copy.shift())};

    return copy.reduce(
        (acc, suite) => ({
            type: 'OR',
            right: acc,
            left: {type: 'EQ', key: 'id', value: parseId(suite)},
        }),
        right,
    );
};

// TODO: заменить на либу
// https://github.yandex-team.ru/search-interfaces/infratest/tree/master/packages/testpalm-api
const testPalmRequest = async (route, options) => {
    const uri = `${TESTPALM_URL}/${route}`;
    const _options = {
        uri,
        method: 'GET',
        rejectUnauthorized: false,
        json: true,
        headers: {
            Authorization: `OAuth ${process.env.TESTPALM_ACCESS_TOKEN}`,
        },
        ...options,
    };

    try {
        log(`trying to make testpalm request...
        url: ${uri}`);
        return await rp(_options);
    } catch (error) {
        if (error.statusCode !== 503) {
            throw new Error(error);
        }

        return [];
    }
};

const updateRun = async (projectId, testcaseId, cases) => {
    const body = {
        id: testcaseId,
        filter: {expression: getFilter(cases)},
    };

    return testPalmRequest(`testsuite/${projectId}`, {body, method: 'PATCH'}).then(() =>
        log(`TestPalm manual run successfully updated
        https://testpalm.yandex-team.ru/${projectId}/testsuite/${testcaseId}`),
    );
};

module.exports = {
    getPlatformSkips,
    getFilter,
    testPalmRequest,
    updateRun,
};
