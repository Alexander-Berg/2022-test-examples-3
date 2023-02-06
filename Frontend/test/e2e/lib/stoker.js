const got = require('got');

const RELEASE_HOST = 'WEB__RENDERER_GOODWIN_RC';

const isRelease = () => Boolean(process.env.RELEASE_SERVICE);

/**
 * Получить хост из стокера
 */
module.exports.getHost = async function() {
    // Для релизного прогона отдаем адрес
    if (isRelease()) return RELEASE_HOST;

    const { STOKER_TOKEN, TRENDBOX_PULL_REQUEST_NUMBER } = process.env;
    const response = await got.post('http://stoker.z.yandex-team.ru/api/stoker.services.Model/getRecord', {
        json: {
            type: 'TAGGED',
            key: `renderer-goodwin-pull-${TRENDBOX_PULL_REQUEST_NUMBER}`,
        },
        responseType: 'json',
        headers: {
            'Content-Type': 'application/json',
            Authorization: STOKER_TOKEN,
        },
        retry: 5,
    });
    const responseHeaders = response.body.content.headers;
    const internalFlags = responseHeaders.find(item => item.key === 'X-Yandex-Internal-Flags');
    const host = JSON.parse(Buffer.from(internalFlags.value, 'base64').toString()).srcrwr.TEMPLATE_PRESEARCH_INTERNAL;

    return host;
};
