'use strict';

const ST_OAUTH_TOKEN_ENV_KEY = 'ST_OAUTH_TOKEN';
const ST_CLOSED_STATUS_KEY = 'closed';

const skipped = require('../skipped.js');
const STApiClient = require('@yandex-int/stapi');

const {partner_desktop: desktop, partner_touch: touch} = skipped;
const cases = desktop.concat(touch);
const tickets = cases.reduce((res, item) => res.add(item.issue), new Set());

console.log(`Found ${tickets.size} tickets in skipped.js\n`);

const getClosedIssues = issues => {
    const client = new STApiClient({
        entrypoint: 'https://st-api.yandex-team.ru/',
        retries: 2,
        timeout: 3000,
    });

    const query = `Key: ${Array.from(issues).join(', ')}`;
    console.log(`==ST Query==\n${query}\n`);

    const session = client.createSession({
        token: process.env[ST_OAUTH_TOKEN_ENV_KEY],
    });

    return new Promise((resolve, reject) => {
        // делаем на одной странице из-за упячки с пагинацией: TOOLSUP-85740
        session.issues.getAll({query, perPage: issues.size}, (err, issues) => {
            if (err) {
                return reject(err);
            }
            const closed = [];
            issues.forEach((err, issue) => {
                if (err) {
                    console.log(err);
                    reject(err);
                } else {
                    if (issue.getStatus().getKey() === ST_CLOSED_STATUS_KEY) {
                        closed.push(issue.getKey());
                    }

                    if (!issues.hasNext()) {
                        resolve(closed);
                    }
                }
            });
        });
    });
};

getClosedIssues(tickets).then(closed => {
    let code = 0;

    console.log('----------');
    if (closed.length) {
        code = 1;

        console.log(`FAIL: Found ${closed.length} closed ticket(s) in skipped.js: ${closed.join(', ')}\n`);
        console.log('Fixed test cases have to be removed from the skip pack');
        console.log('Non-fixed test cases have to be moved to other tickets');
    } else {
        console.log(`OK: There are no closed tickets in skipped.js.`);
    }

    console.log('----------\n');
    process.exit(code);
});
