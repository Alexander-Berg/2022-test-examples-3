const ask = require('asker-as-promised');
const Ajv = require('ajv');
const betterAjvErrors = require('better-ajv-errors');
const root = require('../utils/schemas/root');

const {
    PROTOCOL: protocol = 'https:',
    HOST: host,
    PORT: port,
} = process.env;

const validator = new Ajv({
    allErrors: true,
    jsonPointers: true,
});

describe('Провека структуры', () => {
    if (!host) throw new Error('Не указана переменная окружения HOST! Необходимо для запросов.');

    it('Запрос по фразе "test"', async() => {
        const response = await query({ protocol, host, port, queryParams: { text: 'test' } });
        const items = getItems(response);
        const options = { allowed: ['item', 'wizards_banner', 'wizards_market'] };

        await Promise.all(items.map(item => validateItem(item, options)));
    });

    it('Проверка, что у android относительные ссылки', async() => {
        const response = await query({
            protocol,
            host,
            port,
            queryParams: {
                app_version: 230,
                app_platform: 'android',
                uuid: '246f912eafb970a5fe7fee9f8d09bb06',
                ver: 2,
                type: 'sites',
                text: '%D0%BF%D0%B8%D1%86%D1%86%D0%B0',
                pretty: 1,
                lr: 2,
            },
        });

        const items = getItems(response);
        const urls = items.filter(item => item.url)
            .filter(item => item.subtype && item.subtype === 'bunner')
            .map(item => item.url.click || item.url)
            .filter(item => !item.startsWith('/'));

        assert.equal(urls.length, 0, urls[1]);
    });

    it('Запрос по фразе "Маднна", должно быть испраление опечатки', async() => {
        const response = await query({ protocol, host, port, queryParams: { text: 'Маднна' } });
        const [firstItem, ...items] = getItems(response);
        const options = { allowed: ['item', 'wizards_banner', 'wizards_market'] };

        await Promise.all(
            [validateItem(firstItem, { allowed: ['wizards_misspell'] })]
                .concat(items.map(item => validateItem(item, options)))
        );
    });

    it('Запрос по фразе "Окна века", должна быть реклама', async() => {
        const response = await query({ protocol, host, port, queryParams: { text: 'окна века' } });
        const [firstItem, ...items] = getItems(response);
        const options = { allowed: ['item', 'wizards_banner', 'wizards_market'] };

        await Promise.all(
            [validateItem(firstItem, { allowed: ['wizards_banner'] })]
                .concat(items.map(item => validateItem(item, options)))
        );
    });

    it('Запрос по фразе "test", на второй странице не должно быть дистрибьюции', async() => {
        const response = await query({
            protocol,
            host,
            port,
            queryParams: { text: 'test', p: 1 },
        });
        const items = getItems(response);
        const options = { allowed: ['item', 'wizards_banner'] };

        await Promise.all(items.map(item => validateItem(item, options)));
    });

    it('Запрос по фразе "test" для wp (windows phone), не должно быть дитрибьюции', async() => {
        const response = await query({
            protocol,
            host,
            port,
            queryParams: { text: 'test', app_platform: 'wp' },
        });
        const items = getItems(response);
        const options = { allowed: ['item', 'wizards_banner'] };

        await Promise.all(items.map(item => validateItem(item, options)));
    });

    it('Длинный запрос', async() => {
        const LONG_TEXT =
            'ワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシ' +
            'ワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシ' +
            'ワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシ' +
            'ワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシワタシ';

        const response = await query({
            protocol,
            host,
            port,
            queryParams: { text: LONG_TEXT },
        });
        const items = getItems(response);
        const options = { allowed: ['item', 'wizards_banner', 'wizards_market'] };

        await Promise.all(items.map(item => validateItem(item, options)));
    });
});

async function query({ queryParams = {}, protocol, host, port }) {
    if (!port) {
        switch (protocol) {
            case 'https:': port = 443; break;
            case 'http:': port = 80; break;
            case 'ftp:': port = 21; break;
            default: throw new Error('Port is not defined!');
        }
    }

    const path = '/jsonproxy?' +
        Object.entries(queryParams).map(entry => entry.map(encodeURIComponent).join('=')).join('&');
    const response = await ask({
        protocol,
        host,
        port,
        path,
        method: 'GET',
        requestId: 'jsonproxy-schema',
        timeout: 5000,
        maxRetries: 2,
    });
    const jsonData = JSON.parse(response.data);
    validateJson(root, jsonData);

    return jsonData;
}

function getItems(value) {
    return value.docs[0].list;
}

async function validateItem(itemData, options) {
    const name = itemData.type ?
        itemData.type + (itemData.subtype ? '_' + itemData.subtype : '') :
        'item';

    if (options.allowed && !options.allowed.includes(name)) {
        assert.include(options.allowed, name);
    }
    const path = `../utils/schemas/${name}.json`;
    const jsonSchema = require(path);

    if (itemData.url) {
        const url = itemData.url.click || itemData.url;
        const { statusCode } = await ask({ url });

        if (url.includes('://yabs.yandex.') || url.includes('://appmetrica.yandex.')) {
            // проверяем, что реклама и дистрибьюция возвращают 302
            assert.equal(statusCode, 302, 'Ссылка должна отдлавать ответ редирект с кодом 302!');
        } else {
            // Остальные ссылки должны быть рабочими
            // TODO: возможно стоит сделать проверку, что на отдаваемой html-странице есть редирект
            assert.equal(statusCode, 200, 'Запрос:' + url);
        }
    }

    validateJson(jsonSchema, itemData);
}

function validateJson(scheme, jsonData) {
    const isValid = validator.validate(scheme, jsonData);

    if (!isValid) {
        const errorMessage = betterAjvErrors(scheme, jsonData, validator.errors, { format: 'cli', indent: 2 });
        assert.fail(errorMessage);
    }
}
