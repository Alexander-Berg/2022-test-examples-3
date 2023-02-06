/**
 *  Удаление строк скип-пака по закрытым тикетам.
 *
 *  Для получения OAuth-токена зайдите из под нужного пользователя на
 *    https://oauth.yandex-team.ru/authorize?response_type=token&client_id=5f671d781aca402ab7460fde4050267b
 */

const {ST_OAUTH_TOKEN} = process.env;
const ST_API = new URL('https://st-api.yandex-team.ru/v2/');

const {readFileSync, writeFileSync, readdirSync} = require('fs');
const {join, relative} = require('path');
const ask = require('asker-as-promised');

const reIssue = /^\s*[=]?(\w{3,}-\d+)\s/;

function getSkipIssues() {
    const skipped = require('../skipped.js');
    const values = Object.values(skipped) || [];

    return [...new Set([].concat(...values).map(x => x.issue))];
}

async function getFromTracker(path, query) {
    if (!ST_OAUTH_TOKEN)
        throw Error(
            'Нужна переменная окружения ST_OAUTH_TOKEN, получи токен по адресу https://oauth.yandex-team.ru/authorize?response_type=token&client_id=5f671d781aca402ab7460fde4050267b',
        );

    const response = await ask({
        url: new URL(path, ST_API).href,
        query,
        headers: {
            Authorization: `OAuth ${ST_OAUTH_TOKEN}`,
        },
        timeout: 5000,
        maxRetries: 2,
        isNetworkError: status => status >= 300,
    });

    return JSON.parse(String(response.data));
}

async function filterClosedIssues(issues) {
    const result = [];

    for (let i = 0, step = 100; i < issues.length; i += step) {
        // eslint-disable-next-line no-await-in-loop
        const page = await getFromTracker('issues', {
            fields: 'key,summary,status',
            keys: issues.slice(i, step),
            filter: 'status:closed', // не работает :(
            perPage: step,
        });

        result.push(...page.filter(x => x.status.key === 'closed'));
    }

    return result;
}

function patchFile(path, issues) {
    console.warn('\n/// %s\n', path);
    const issuesSet = new Set(issues);
    const text = readFileSync(path, {encoding: 'utf-8'}).split(/\r?\n/);
    const result = [];
    const buffer = [];
    let bufferHasSkips = false;
    let changes = 0;

    if (text.slice(-1)[0] !== '') {
        text.push('');
    }

    for (const line of text) {
        if (line.trim() === '') {
            if (buffer.length) {
                if (!bufferHasSkips || buffer.some(x => reIssue.test(x))) {
                    result.push(...buffer);
                } else {
                    console.warn(buffer.join('\n'), '\n');
                }
                buffer.length = 0;
                bufferHasSkips = false;
                result.push(line);
            }
        } else if (reIssue.test(line)) {
            const [, issue] = reIssue.exec(line);

            bufferHasSkips = true;

            if (issuesSet.has(issue)) {
                console.warn(line);
                changes++;
            } else {
                buffer.push(line);
            }
        } else {
            buffer.push(line);
        }
    }

    if (!changes) {
        console.warn('/ no file changes.');
    } else {
        writeFileSync(path, result.join('\n'));
    }

    return changes;
}

function patchFiles(issues) {
    let changes = 0;

    for (const name of readdirSync(join(__dirname, '..'))) {
        if (/^skipped[.].+[.]txt$/.test(name)) {
            changes += patchFile(relative('.', join(__dirname, '..', name)), issues);
        }
    }
    console.warn('\n///.');

    return changes;
}

(async function main() {
    const args = process.argv.slice(2);
    let changes;

    if (args.length > 0) {
        console.warn('Removing by arguments...');

        changes = patchFiles(args);
    } else {
        console.warn('Removing closed issues...');

        const closed = await filterClosedIssues(getSkipIssues());
        if (!closed.length) {
            console.error(' No issues!');
            process.exit(1); // all are open
        }
        for (const {key, summary} of closed) {
            console.warn(' - %s %s', key, summary);
        }

        changes = patchFiles(closed.map(x => x.key));
    }

    if (!changes) {
        process.exit(1); // no changes
    }
})().catch(e => {
    console.error(e);
    process.exit(2);
});
