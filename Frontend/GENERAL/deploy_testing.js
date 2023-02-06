/* eslint-disable no-console */
const fetch = require('node-fetch');

const getSecret = require('./get_secrets');
const user = process.env.tunneler_user || process.env.USER;
const qloudOauthToken = getSecret(user, 'robot_mop_qloud_token');

const version = require('../package.json').version;

(async() => {
    try {
        console.log(`Переключаем тестинг на версию ${version}`);
        const res = await fetch(
            'https://femida.test.yandex-team.ru/_api/frontend/change_version/',
            {
                method: 'post',
                body: JSON.stringify({ version }),
                headers: {
                    Authorization: `OAuth ${qloudOauthToken}`,
                    'Content-Type': 'application/json',
                },
            });
        if (res.status >= 400) {
            const jsonRes = await res.json();
            console.error(`Ошибка при переключении версии: ${JSON.stringify(jsonRes, null, 4)}`);
            process.exit(1);
        }
        console.log(`Тестинг переключен на версию ${version}`);
    } catch (e) {
        console.error(`Ошибка при переключении версии: ${e}`);
    }
})();
