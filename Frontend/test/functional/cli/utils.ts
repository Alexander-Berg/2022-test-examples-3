/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import * as proxyquire from 'proxyquire';
import { createUser, wipeDatabase } from '../_helpers';

test.beforeEach(async() => {
    await wipeDatabase();
});

test('askForUserWithEmail should return valid user', async t => {
    await createUser({ id: '0001' });

    const fake = sinon.fake;
    const testEmail = 'user@test.com';

    const utils = proxyquire('../../../cli/utils', {
        '../services/tvm': {
            getServiceTickets: fake.resolves({ blackbox: {} }),
        },
        inquirer: {
            prompt: fake.resolves({ email: testEmail }),
        },
        '../services/blackbox': {
            getUserInfo: fake.resolves({
                body: {
                    users: [
                        {
                            uid: {
                                value: '0001',
                            },
                        },
                    ],
                },
            }),
        },
    });

    const user = await utils.askForUserWithEmail();

    t.is(user.id, '0001');
});

test('askForUsersWithEmails should return valid users', async t => {
    await createUser({ id: '0001' });
    await createUser({ id: '0002' });

    const fake = sinon.fake;
    const testEmail1 = 'user1@test.com';
    const testEmail2 = 'user2@test.com';

    const utils = proxyquire('../../../cli/utils', {
        '../services/tvm': {
            getServiceTickets: fake.resolves({ blackbox: {} }),
        },
        inquirer: {
            prompt: fake.resolves({ emails: `${testEmail1} ${testEmail2}` }),
        },
        '../services/blackbox': {
            getUserInfo: async({ login }: { login: string }) => {
                return {
                    body: {
                        users: [
                            {
                                uid: {
                                    value: login === testEmail1 ? '0001' : '0002',
                                },
                            },
                        ],
                    },
                };
            },
        },
    });

    const users = await utils.askForUsersWithEmails();

    t.is(users[0].id, '0001');
    t.is(users[1].id, '0002');
});
