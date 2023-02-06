/* eslint-disable max-len */

'use strict';

const request = require('request');

const Client = require('./../../lib/client');

describe('Example of using mock', () => {
    describe('request to \'api.content.market.yandex.ru/v1/search.json\'', () => {
        const mock = {
            hostname: 'https://api.content.market.yandex.ru',
            path: '/v1/search.json',
            query:
                'text=5352013308&adult=0&count=30&fields=discounts&price_min=231&price_max=82600&check_spelling=1&category_id=90829&geo_id=213&clid=2210590&req_id=it9xrubxy802866zjiu5bwo84m3zovh7&search_type=isbn',
            result: {
                status: '200',
                body: {
                    searchResult: {
                        page: 1,
                        count: 1,
                        total: 1,
                        requestParams: {
                            text: 5352013308,
                            actualText: 5352013308,
                            checkSpelling: true
                        },
                        results: [
                            {
                                model: {
                                    id: 8239980,
                                    name: 'name',
                                    description: 'description'
                                },
                                category: {
                                    id: 90829,
                                    type: 'GENERAL',
                                    name: 'книги'
                                }
                            }
                        ]
                    }
                }
            }
        };

        function middleware(req, res, next) {
            request(
                {
                    method: 'GET',
                    uri:
                        'https://api.content.market.yandex.ru/v1/search.json?text=5352013308&adult=0&count=30&fields=discounts&price_min=231&price_max=82600&check_spelling=1&category_id=90829&geo_id=213&clid=2210590&req_id=it9xrubxy802866zjiu5bwo84m3zovh7&search_type=isbn'
                },
                (error, response, body) => {
                    if (error) {
                        throw new Error(error);
                    } else {
                        res.statusCode = response.statusCode;
                        res.body = body;
                    }

                    next();
                }
            );
        }

        test('should retun correct data', async (done) => {
            const client = new Client(null, null, false, mock);
            await client.request([
                middleware,
                (req, res) => {
                    try {
                        expect(res.statusCode).toBe(parseInt(mock.result.status, 10));
                        expect(JSON.parse(res.body)).toEqual(mock.result.body);
                    } catch (err) {
                        return done(err);
                    }

                    res.json();
                    done();
                }
            ]);
        });
    });
});
