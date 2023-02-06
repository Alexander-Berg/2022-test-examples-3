const ask = require('asker-as-promised');
const config = require('../skipped.js');

// https://wiki.yandex-team.ru/testpalm/testpalmdoc/api/
const TP_API = new URL('https://testpalm-api.yandex-team.ru/');
const {TESTPALM_OAUTH_API_TOKEN} = process.env;
const PROJECT = 'marketmbi';

async function getFromTestPalm(path, query) {
    const response = await ask({
        url: new URL(path, TP_API).href,
        query,
        headers: {
            Authorization: `OAuth ${TESTPALM_OAUTH_API_TOKEN}`,
        },
        timeout: 15000,
        maxRetries: 2,
        isNetworkError: status => status >= 300,
    });

    return JSON.parse(String(response.data));
}

describe('Проверка по данным TestPalm', () => {
    jest.setTimeout(40e3);

    it('Отсутствие SOX-тестов', async () => {
        const soxId = await getFromTestPalm(`testcases/${PROJECT}`, {
            include: 'id',
            expression: `{"type":"EQ","key":"attributes.56baf83b88955028cadd30e7","value":"yes"}`,
        });
        const soxSet = new Set(soxId.map(x => `${PROJECT}-${x.id}`));
        const failures = [];

        expect(soxSet.size).toBeGreaterThan(0); // что-то нашлось

        for (const [name, skips] of Object.entries(config)) {
            for (const {issue, cases} of skips) {
                for (const {id, fullName} of cases) {
                    if (soxSet.has(id)) {
                        process.stderr.write(`[SOX] ${name}: ${issue} ${id} ${fullName}\n`);
                        if (name !== 'adv') failures.push(id);
                    }
                }
            }
        }

        expect(failures.join(' ')).toBeFalsy(); // список запретных SOX тестов
    });
});
