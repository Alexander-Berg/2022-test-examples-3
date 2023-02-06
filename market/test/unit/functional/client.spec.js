'use strict';

const request = require('request');

const Client = require('./../../functional/lib/client');

describe('Client', () => {
    describe('mocks', () => {
        const apiYaMock = {
            hostname: 'https://ya.ru',
            result: {
                status: 200,
                body: {
                    a: 'b'
                }
            }
        };

        const apiYaMockWithQuery = {
            hostname: 'https://ya.ru',
            path: '',
            query: {
                b: {
                    value: /alu/,
                    text: 'text'
                },
                c: /st/
            },
            result: {
                status: 200,
                body: {
                    d: 'd'
                }
            }
        };

        function stubYaMiddleware(req, res, next) {
            request(
                {
                    method: 'GET',
                    uri: 'https://ya.ru',
                    qs: {
                        a: 'a',
                        b: {
                            value: 'value',
                            text: 'text'
                        },
                        c: 'test'
                    }
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

        test('should mock \'https://ya.ru\' requests (client mock)', async (done) => {
            const client = new Client(null, null, false, apiYaMock);
            await client.request([
                stubYaMiddleware,
                (req, res) => {
                    try {
                        expect(res.statusCode).toBe(apiYaMock.result.status);
                        expect(JSON.parse(res.body)).toEqual(apiYaMock.result.body);
                    } catch (err) {
                        return done(err);
                    }

                    res.json();
                    done();
                }
            ]);
        });

        test('should mock several \'https://ya.ru\' requests (request mock)', async (done) => {
            const client = new Client(null, null, false);
            await client.request([
                stubYaMiddleware,
                (req, res) => {
                    try {
                        expect(res.statusCode).toBe(apiYaMock.result.status);
                        expect(res.body).not.toEqual(JSON.stringify(apiYaMock.result.body));
                    } catch (err) {
                        return done(err);
                    }

                    res.json();
                }
            ]);

            await client.request(
                [
                    stubYaMiddleware,
                    (req, res) => {
                        try {
                            expect(res.statusCode).toBe(apiYaMock.result.status);
                            expect(JSON.parse(res.body)).toEqual(apiYaMock.result.body);
                        } catch (err) {
                            return done(err);
                        }

                        res.json();
                    }
                ],
                {},
                apiYaMock
            );

            await client.request([
                stubYaMiddleware,
                (req, res) => {
                    try {
                        expect(res.statusCode).toBe(apiYaMock.result.status);
                        expect(res.body).not.toEqual(JSON.stringify(apiYaMock.result.body));
                    } catch (err) {
                        return done(err);
                    }

                    done();
                    res.json();
                }
            ]);
        });

        test('should mock \'https://ya.ru\' requests (request mock with query)', async (done) => {
            const client = new Client(null, null, false, apiYaMockWithQuery);
            await client.request([
                stubYaMiddleware,
                (req, res) => {
                    try {
                        expect(res.statusCode).toBe(apiYaMock.result.status);
                        expect(JSON.parse(res.body)).toEqual(apiYaMockWithQuery.result.body);
                    } catch (err) {
                        return done(err);
                    }

                    done();
                    res.json();
                }
            ]);
        });
    });
});
