const sinon = require('sinon');
const { expect } = require('chai');
const moment = require('moment');

const catchError = require('tests/helpers/catchError').generator;
const checkBan = require('middleware/ban');

const dbHelper = require('tests/helpers/clear');

const banFactory = require('tests/factory/bansFactory');
const usersFactory = require('tests/factory/usersFactory');

describe('Check ban middleware', () => {
    beforeEach(dbHelper.clear);

    it('should call `next` when user is not authorized', function *() {
        const context = { state: { user: {} } };
        const spy = sinon.spy();

        yield checkBan.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should call `next` when user has no global id', function *() {
        const authType = { id: 2, code: 'web' };

        yield usersFactory.createWithRelations({
            id: 1,
            uid: 123,
            globalUserId: null
        }, { authType });

        const context = {
            params: { examIdentity: 1 },
            state: {
                user: { uid: { value: 123 } },
                authType: 'web'
            }
        };
        const spy = sinon.spy();

        yield checkBan.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should call `next` when user and global user is not banned', function *() {
        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            {
                globalUser: { id: 10, isBanned: false, actualLogin: 'actual' },
                authType: { id: 2, code: 'web' }
            }
        );

        const context = {
            params: { examIdentity: 1 },
            state: {
                user: {
                    uid: { value: 123 },
                    login: 'actual'
                },
                authType: 'web'
            }
        };
        const spy = sinon.spy();

        yield checkBan.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should call `next` when ban is expired', function *() {
        const authType = { id: 2, code: 'web' };
        const globalUser = { id: 10, actualLogin: 'actual' };
        const expiredDate = moment(Date.now()).subtract(1, 'month').toDate();

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        yield banFactory.createWithRelations({
            id: 2,
            action: 'ban',
            isLast: true,
            expiredDate
        }, {
            trialTemplate: { id: 1 },
            globalUser,
            admin: { id: 1234 }
        });

        const context = {
            params: { examIdentity: 1 },
            state: {
                user: {
                    uid: { value: 123 },
                    login: 'actual'
                },
                authType: 'web'
            }
        };

        const spy = sinon.spy();

        yield checkBan.call(context, function *() {
            spy();
            yield {};
        });

        expect(spy.calledOnce).to.be.true;
    });

    it('should throw error when global user is banned', function *() {
        const authType = { id: 2, code: 'web' };
        const globalUser = { id: 10, isBanned: true };

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        const context = {
            params: { examIdentity: 1 },
            state: {
                user: { uid: { value: 123 } },
                authType: 'web'
            }
        };
        const error = yield catchError(checkBan.bind(context, {}));

        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('User is banned');
        expect(error.options).to.deep.equal({
            internalCode: '403_UIB',
            authTypeCode: 'web',
            uid: 123
        });
    });

    it('should throw error when user is banned for exam', function *() {
        const authType = { id: 2, code: 'web' };
        const globalUser = { id: 10 };
        const expiredDate = moment(Date.now()).add(1, 'month').toDate();

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        yield banFactory.createWithRelations({
            id: 2,
            action: 'ban',
            isLast: true,
            expiredDate
        }, {
            trialTemplate: { id: 1 },
            globalUser,
            admin: { id: 1234 }
        });

        const context = {
            params: { examIdentity: 1 },
            state: {
                user: { uid: { value: 123 } },
                authType: 'web'
            }
        };
        const error = yield catchError(checkBan.bind(context, {}));

        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('User is banned');
        expect(error.options).to.deep.equal({
            internalCode: '403_UIB',
            authTypeCode: 'web',
            uid: 123
        });
    });

    it('should throw error when user login is not actual', function *() {
        const authType = { id: 2, code: 'web' };
        const globalUser = { id: 10, actualLogin: 'actual' };

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        const context = {
            params: { examIdentity: 1 },
            state: {
                user: {
                    uid: { value: 123 },
                    login: 'notActual'
                },
                authType: 'web'
            }
        };
        const error = yield catchError(checkBan.bind(context, {}));

        expect(error.statusCode).to.equal(403);
        expect(error.message).to.equal('Login is not actual');
        expect(error.options).to.deep.equal({
            internalCode: '403_LAC',
            authTypeCode: 'web',
            uid: 123
        });
    });
});
