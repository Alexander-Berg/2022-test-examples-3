const chai = require('chai');
const request = require('request-promise');
const sbsRobotToken = require('config').sbsRobotToken;

describe('HTTP API', () => {
    const baseHost = 'https://test.sbs.yandex-team.ru';
    const commonRequestOptions = {
        headers: {
            Authorization: `OAuth ${sbsRobotToken}`,
        },
    };

    describe('search', () => {
        describe('api/experiment/:expId/results', () => {
            const baseUrl = `${baseHost}/api/experiment/69543/v2/results`;

            it('без позапросного анализа', () => {
                return request(baseUrl, commonRequestOptions)
                    .then((res) => chai.assert.deepEqual(JSON.parse(res), require('./fixtures/serp/69543-results.json')));
            });

            it('с позапросным анализом с ограничением на кол-во комментариев ?queries=1', () => {
                return request(`${baseUrl}?queries=1`, commonRequestOptions)
                    .then((res) => chai.assert.deepEqual(JSON.parse(res), require('./fixtures/serp/69543-results-queries.json')));
            });

            it('с позапросным анализом без ограничения на кол-во комментариев ?queries=1&queries-no-limit=1', () => {
                return request(`${baseUrl}?queries=1&queries-no-limit=1`, commonRequestOptions)
                    .then((res) => chai.assert.deepEqual(JSON.parse(res), require('./fixtures/serp/69543-results-queries-no-limit.json')));
            });
        });

        describe.skip('nirvana', () => {
            const url = `${baseHost}/api/experiment/69543/config`;

            it('/config', () => {
                return request(url, commonRequestOptions)
                    .then((res) => chai.assert.deepEqual(JSON.parse(res), require('./fixtures/serp/69543-config.json')));
            });
        });
    });

    describe('design', () => {
    });

    describe('scenario', () => {
    });

    describe('poll', () => {
    });
});
