const assert = require('assert');
const catchErrorAsync = require('catch-error-async');
const uuid = require('uuid/v1');

const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Subscription = require('models/subscription');

const testDbType = DbType.internal;

describe('Subscription model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a subscription', async() => {
            const unsubscribeCode = uuid();

            await factory.subscription.create({
                id: 4,
                accountId: { id: 5 },
                tagId: { id: 7 },
                type: 'news',
                isActive: true,
                unsubscribeCode,
                createdAt: new Date(2018, 10, 15),
            });

            const actual = await Subscription.findOne({ id: 4, scope: 'one', dbType: testDbType });
            const expected = {
                id: 4,
                accountId: 5,
                tagId: 7,
                type: 'news',
                isActive: true,
                unsubscribeCode,
                createdAt: new Date(2018, 10, 15),
            };

            assert.deepStrictEqual(actual.toJSON(), expected);
        });

        it('should throw if subscription is not found', async() => {
            const error = await catchErrorAsync(
                Subscription.findOne.bind(Subscription), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.strictEqual(error.message, 'Subscription not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('create', () => {
        it('should create a subscription', async() => {
            await factory.tag.create({ id: 8, slug: 'frontend', title: 'Фронтенд' });
            await factory.account.create({ id: 8 });

            const data = { id: 5, tagId: 8, accountId: 8 };
            const subscription = new Subscription(data, { dbType: testDbType });
            const subscriptionId = await subscription.create({ dbType: testDbType });

            const actual = await Subscription.findOne({ id: subscriptionId, scope: 'one', dbType: testDbType });

            assert.strictEqual(actual.id, data.id);
        });

        it('should throw if user has already subscribed', async() => {
            await factory.subscription.create({
                id: 4,
                accountId: { id: 5 },
                tagId: { id: 7 },
            });

            const subscription = new Subscription({ accountId: 5, tagId: 7 }, { dbType: testDbType });

            const error = await catchErrorAsync(
                subscription.create.bind(subscription, { dbType: testDbType }),
            );

            assert.strictEqual(error.message, 'User has already subscribed');
            assert.strictEqual(error.statusCode, 409);
            assert.deepStrictEqual(error.options, {
                internalCode: '409_UAR',
                tagId: 7,
                accountId: 5,
            });
        });
    });
});
