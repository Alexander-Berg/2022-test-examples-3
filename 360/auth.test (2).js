'use strict';

const s = require('@ps-int/mail-lib').helpers.serializr;
const authSchema = require('./auth.js');
const deserialize = s.deserialize.bind(s, authSchema);

describe('authSchema', () => {
    it('copies and renames properties', () => {
        const result = deserialize({
            account_information: {
                account: {
                    composeCheck: 1,
                    country: 'ru',
                    domain: 2,
                    karma: {
                        value: '123',
                        status: '456'
                    },
                    language: 3,
                    login: 4,
                    mailDataBase: 5,
                    serviceUserId: 6,
                    userId: 7,
                    account: 8
                }
            }
        });

        expect(result).toEqual({
            'compose-check': 1,
            'countryCode': 'ru',
            'domain': 2,
            'karma': {
                value: 123,
                status: 456
            },
            'locale': 3,
            'login': 4,
            'mdb': 5,
            'suid': 6,
            'uid': 7,
            'yandex_account': 8
        });
    });

    describe('timezone', () => {
        it('contains timezone offset in minutes', () => {
            const result = deserialize({
                account_information: {
                    account: {
                        timeZone: { offset: 60 }
                    }
                }
            });

            expect(result).toEqual({
                tz_offset: 1
            });
        });

        it('contains timezone name', () => {
            const result = deserialize({
                account_information: {
                    account: {
                        timeZone: { timezone: 'Europe/Moscow' }
                    }
                }
            });

            expect(result).toEqual({
                timezone: 'Europe/Moscow'
            });
        });
    });

    describe('emails', () => {
        it('contains user emails', () => {
            const result = deserialize({
                account_information: {
                    addresses: {
                        internalAddresses: [
                            'foo@example.com',
                            'bar@example.net'
                        ]
                    }
                }
            });

            expect(result).toEqual({
                emails: [
                    { login: 'foo', domain: 'example.com' },
                    { login: 'bar', domain: 'example.net' }
                ]
            });
        });
    });
});
