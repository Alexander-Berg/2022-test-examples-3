/* eslint-disable no-console */
const { PR_NUMBER } = process.env;
const prChecker = require('../../../../tools/ci/pr-checker.js');
const gh = require('../../../../tools/ci/gh.js');

const cmd = require('../../../../tools/ci/libs/cmd.js');

const wait = (ms) => new Promise(resolve => {
    setTimeout(resolve, ms);
});

const RETRY_DELAY = 3 * 60 * 1000; // ms
const RETRY_LIMIT = 20;

const checkStatus = async (host, count = 0) => {
    if (count === RETRY_LIMIT) {
        throw new Error('retry limit reached');
    }

    await wait(RETRY_DELAY);

    await cmd('env');
    const status = await cmd(`curl -I https://${host} 2>/dev/null | head -n 1 | cut -d$' ' -f2`);

    console.log({status});
    if (status === '500' || status === '502') {
        console.log('version is not deployed yet...', new Date());
        return checkStatus(host, ++count);
    }

    console.log('version deployed!', new Date());
    return true;
};

(async () => {
    const pr = await gh.getPullRequest(PR_NUMBER);
    const host = `pr-${PR_NUMBER}.qa.mail.yandex.ru`;
    console.log({ pr });

    if (pr.message === 'Not Found') {
        console.log('PR not found');
        process.exit(0);
    }

    await prChecker(pr);

    await checkStatus(host);

    try {
        await cmd(`AUTH_TOKEN=${process.env.SANDBOX_OAUTH_TOKEN} TASK_TYPE=BROWSER_PERF_RECORD_WPR_ARCHIVE COMMIT=${pr.head.sha} TOUCH_URL=${'https://' + host + '/touch/'} make -s ci-record-wpr`);
    } catch (e) {
        console.error(e);
    }
    process.exit(0);
})();
