const assert = require('assert');
const nock = require('nock');
const sinon = require('sinon');
const _ = require('lodash');

const db = require('db');
const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

const RegistrationsStatusesService = require('worker/registrationsStatusesService');

const testDbType = DbType.internal;

describe('Worker mail distribution service', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('sync', () => {
        it('should synchronize events registration status', async() => {
            const date = new Date('2012-02-16T07:00:00.000Z');
            const clock = sinon.useFakeTimers(date.getTime());

            await factory.event.create([
                {
                    slug: 'registration-is-started',
                    registrationStatus: 'opened_later', // Должен быть opened
                    registrationStartDate: new Date('2012-02-15T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-26T04:00:00.000Z'),
                },
                {
                    slug: 'registration-is-closed',
                    registrationStatus: 'opened', // Должен быть closed
                    registrationStartDate: new Date('2012-02-10T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-16T04:00:00.000Z'),
                },
                {
                    slug: 'registration-is-started2',
                    registrationStatus: 'opened',
                    registrationStartDate: new Date('2012-02-10T04:00:00.000Z'),
                    registrationEndDate: new Date('2012-02-26T04:00:00.000Z'),
                },
            ].map((item, index) => ({
                id: index + 1,
                registrationDateIsConfirmed: true,
                autoControlRegistration: true,
                ...item,
            })));
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const { sync } = await RegistrationsStatusesService.sync({ dbType: testDbType });

            assert(sync);

            const events = await db.event.findAll({ order: [['id', 'ASC']] });

            const actual = _.map(events, _.partialRight(_.pick, ['slug', 'registrationStatus']));

            assert.strictEqual(actual.length, 3);
            assert.deepStrictEqual(actual, [
                { slug: 'registration-is-started', registrationStatus: 'opened' },
                { slug: 'registration-is-closed', registrationStatus: 'closed' },
                { slug: 'registration-is-started2', registrationStatus: 'opened' },
            ]);

            clock.restore();
        });
    });
});
