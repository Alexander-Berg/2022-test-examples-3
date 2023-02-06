/* eslint-disable */
import test from 'ava';
import { makeGetRequest } from './_heplers';
import { wipeDatabase } from '../../_helpers';

test.beforeEach(wipeDatabase);

test('get info', async t => {
    const res = await makeGetRequest('info');
    t.deepEqual(res.body, {
        code: 0,
        roles: {
            slug: 'paskills',
            name: 'Консоль разработчика Яндекс.Диалогов',
            values: {
                admin: 'Администратор',
            },
        },
        fields: [
            {
                slug: 'passport-login',
                name: {
                    ru: 'Паспортный логин',
                    en: 'Passport login',
                },
                type: 'passportlogin',
                required: true,
            },
        ],
    });
});
