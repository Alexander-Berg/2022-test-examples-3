const assert = require('assert');
const request = require('supertest');
const config = require('yandex-cfg');
const _ = require('lodash');

const app = require('app');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const db = require('db');

const { nockTvmCheckTicket } = require('tests/mocks');
const ticketBody = '3:serv:CNZCEIrCjOcFIgcIwY56EN8B:GWQMLZMA6uz6zSOPDS';

describe('IDM routes', () => {
    beforeEach(cleanDb);

    it('/info/', async() => {
        nockTvmCheckTicket({ src: config.idm.tvmSource });
        await factory.group.create([
            { id: 1, slug: 'group1', name: 'тест 1' },
            { id: 2, slug: 'group2', name: 'тест 2' },
        ]);

        await request(app.listen())
            .get('/v1/idm/info/')
            .set('x-ya-service-ticket', ticketBody)
            .expect('Content-Type', /json/)
            .expect(200)
            .expect(({ body }) => {
                assert.deepStrictEqual(body, {
                    code: 0,
                    roles: {
                        ...config.idm.roles,
                    },
                });

                assert.deepStrictEqual(_.get(body, 'roles.values.groups.roles.values.group1'),
                    {
                        name: 'тест 1',
                        roles: {
                            slug: 'grand',
                            name: 'Уровень доступа',
                            values: { groupManager: 'Менеджер для группы', groupOwner: 'Заказчик для группы' },
                        },
                    },
                );
            });
    });

    it('/get-all-roles/', async() => {
        await factory.userRole.create([
            { login: 'saaaaaaaaasha', role: 'viewer' },
            { login: 'zhigalov', role: 'viewer' },
            { login: 'zhigalov', role: 'admin' },
        ]);

        nockTvmCheckTicket({ src: config.idm.tvmSource });

        await request(app.listen())
            .get('/v1/idm/get-all-roles/')
            .set('x-ya-service-ticket', ticketBody)
            .expect('Content-Type', /json/)
            .expect(200)
            .expect({
                code: 0,
                users: [
                    {
                        login: 'saaaaaaaaasha',
                        roles: [{ role: 'viewer' }],
                    },
                    {
                        login: 'zhigalov',
                        roles: [{ role: 'admin' }, { role: 'viewer' }],
                    },
                ],
            });
    });

    it('/add-role/', async() => {
        nockTvmCheckTicket({ src: config.idm.tvmSource });

        await request(app.listen())
            .post('/v1/idm/add-role/')
            .set('x-ya-service-ticket', ticketBody)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .send('login=saaaaaaaaasha&role={"role":"viewer"}')
            .expect('Content-Type', /json/)
            .expect(200)
            .expect({ code: 0 });

        const actual = await db.userRole.findAll({ raw: true });

        assert.equal(actual.length, 1);
        assert.equal(actual[0].role, 'viewer');
        assert.equal(actual[0].login, 'saaaaaaaaasha');
    });

    it('/remove-role/', async() => {
        await factory.userRole.create({
            login: 'zhigalov',
            role: 'viewer',
        });

        nockTvmCheckTicket({ src: config.idm.tvmSource });

        await request(app.listen())
            .post('/v1/idm/remove-role/')
            .set('x-ya-service-ticket', ticketBody)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .send('login=zhigalov&role={"role":"viewer"}')
            .expect('Content-Type', /json/)
            .expect(200)
            .expect({ code: 0 });

        const actual = await db.userRole.findAll();

        assert.equal(actual.length, 0);
    });
});
