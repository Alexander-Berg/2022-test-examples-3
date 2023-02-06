const assert = require('assert');
const sinon = require('sinon');
const catchError = require('catch-error-async');

const idm = require('middlewares/idm');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const { DbType } = require('db/constants');

const testDbType = DbType.internal;

describe('Idm middleware', () => {
    beforeEach(cleanDb);

    describe('admin', () => {
        it('should grant access', async() => {
            await factory.userRole.create({
                login: 'superilya',
                role: 'admin',
            });

            const ctx = { state: { user: { login: 'superilya' } }, dbType: testDbType };
            const next = sinon.spy();

            await idm.admin(ctx, next);

            assert(next.calledOnce);
        });

        it('should throw error', async() => {
            const ctx = { state: { user: { login: 'superilya' } }, dbType: testDbType };
            const next = sinon.spy();

            const error = await catchError(idm.admin, ctx, next);

            assert(next.notCalled);
            assert.equal(error.message, 'User has no access');
            assert.equal(error.statusCode, 403);
            assert.deepEqual(error.options, {
                internalCode: '403_UNA',
                login: 'superilya',
            });
        });
    });

    describe('viewer', () => {
        it('should grant access', async() => {
            await factory.userRole.create({
                login: 'superilya',
                role: 'viewer',
            });

            const ctx = { state: { user: { login: 'superilya' } }, dbType: testDbType };
            const next = sinon.spy();

            await idm.viewer(ctx, next);

            assert(next.calledOnce);
        });

        it('should grant access if user has admin role', async() => {
            await factory.userRole.create({
                login: 'superilya',
                role: 'admin',
            });

            const ctx = { state: { user: { login: 'superilya' } }, dbType: testDbType };
            const next = sinon.spy();

            await idm.viewer(ctx, next);

            assert(next.calledOnce);
        });
    });
});
