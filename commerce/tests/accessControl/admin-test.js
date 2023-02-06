require('co-mocha');

const catchErrorGenerator = require('tests/helpers/catchError').generator;
const catchErrorFunc = require('tests/helpers/catchError').func;

const { expect } = require('chai');
const dbHelper = require('tests/helpers/clear');

const AccessControl = require('accessControl/admin');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const locksFactory = require('tests/factory/locksFactory');

const Authority = require('models/authority');

describe('AdminAccessControl', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    describe('`hasAdminAccess`', () => {
        it('should throw 401 when user not authorized', function *() {
            const accessControl = new AccessControl();
            const error = yield catchErrorGenerator(accessControl.hasAdminAccess.bind(accessControl));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not admin', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });
            const error = yield catchErrorGenerator(accessControl.hasAdminAccess.bind(accessControl));

            expect(error.message).to.equal('User is not admin');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_NAD' });
        });

        it('should success for admin', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'admin' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAdminAccess();
        });
    });

    describe('`hasAnalystAccess`', () => {
        it('should throw 401 when user not authorized', function *() {
            const accessControl = new AccessControl();
            const error = yield catchErrorGenerator(accessControl.hasAnalystAccess.bind(accessControl));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not analyst', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });
            const error = yield catchErrorGenerator(accessControl.hasAnalystAccess.bind(accessControl));

            expect(error.message).to.equal('User is not analyst');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_NAN' });
        });

        it('should success for analyst', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'analyst' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAnalystAccess();
        });
    });

    describe('`hasDeveloperAccess`', () => {
        it('should throw 401 when user not authorized', function *() {
            const accessControl = new AccessControl();
            const error = yield catchErrorGenerator(accessControl.hasDeveloperAccess.bind(accessControl));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not developer', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });
            const error = yield catchErrorGenerator(accessControl.hasDeveloperAccess.bind(accessControl));

            expect(error.message).to.equal('User is not developer');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_UND', uid: '1234567890' });
        });

        it('should success for developer', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'developer' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasDeveloperAccess();
        });
    });

    describe('`hasAnyAccess`', () => {
        function successTest(code) {
            return function *() {
                const admin = { uid: 1029384756 };
                const role = { code };

                yield adminsToRolesFactory.createWithRelations({}, { admin, role });

                const accessControl = new AccessControl({ user: { uid: { value: '1029384756' } } });
                const authority = yield Authority.find(accessControl.uid);

                accessControl.hasAnyAccess(authority);
            };
        }

        it('should success for developer', successTest('developer'));

        it('should success for analyst', successTest('analyst'));

        it('should success for admin', successTest('admin'));

        it('should success for assessor', successTest('assessor'));

        it('should success for editor', successTest('editor'));

        it('should throw 403 when user does not have any role', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1029384756' } } });
            const authority = yield Authority.find(accessControl.uid);
            const error = catchErrorFunc(accessControl.hasAnyAccess.bind(accessControl, authority));

            expect(error.message).to.equal('User has no any access');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_NAA' });
        });
    });

    describe('`hasAccessToLockExam`', () => {
        it('should return `undefined` when there are no records of locking', function *() {
            const accessControl = new AccessControl({ user: { login: 'dotokoto' } });
            const unlockDate = yield accessControl.hasAccessToLockExam(2);

            expect(unlockDate).to.be.undefined;
        });

        it('should return `null` when user is current editor', function *() {
            yield locksFactory.createWithRelations(
                { id: 3 },
                { trialTemplate: { id: 2 }, admin: { login: 'dotokoto' } }
            );

            const accessControl = new AccessControl({ user: { login: 'dotokoto' } });
            const unlockDate = yield accessControl.hasAccessToLockExam(2);

            expect(unlockDate).to.be.null;
        });

        it('should return correct `unlockDate` when exam is completed edit', function *() {
            const unlockDate = new Date(2019, 1, 5);

            yield locksFactory.createWithRelations(
                { id: 3, unlockDate },
                { trialTemplate: { id: 2 }, admin: { login: 'dotokoto' } }
            );

            const accessControl = new AccessControl({ user: { login: 'rinka' } });
            const actual = yield accessControl.hasAccessToLockExam(2);

            expect(actual).to.deep.equal(unlockDate);
        });

        it('should throw 403 when exam is already being edited by another admin', function *() {
            yield locksFactory.createWithRelations(
                { id: 3 },
                { trialTemplate: { id: 2 }, admin: { login: 'anyok' } }
            );

            const accessControl = new AccessControl({ user: { login: 'dotokoto' } });
            const error = yield catchErrorGenerator(accessControl.hasAccessToLockExam.bind(accessControl, 2));

            expect(error.message).to.equal('Exam is already being edited');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_EAE', login: 'anyok' });
        });
    });

    describe('`hasSupportAccess`', () => {
        it('should throw 401 when user not authorized', function *() {
            const accessControl = new AccessControl();
            const error = yield catchErrorGenerator(accessControl.hasSupportAccess.bind(accessControl));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not admin or support', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });
            const error = yield catchErrorGenerator(accessControl.hasSupportAccess.bind(accessControl));

            expect(error.message).to.equal('User has no support access');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_UNS' });
        });

        it('should success for admin', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'admin' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasSupportAccess();
        });

        it('should success for support', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'support' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasSupportAccess();
        });
    });

    describe('`hasAccessToEditExam`', () => {
        it('should throw 401 when user not authorized', function *() {
            const accessControl = new AccessControl();
            const error = yield catchErrorGenerator(accessControl.hasAccessToEditExam.bind(accessControl));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not admin or editor', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });
            const error = yield catchErrorGenerator(accessControl.hasAccessToEditExam.bind(accessControl));

            expect(error.message).to.equal('User has no editor access');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_UEA' });
        });

        it('should success for admin', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'admin' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAccessToEditExam();
        });

        it('should success for editor', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'editor' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAccessToEditExam();
        });
    });

    describe('`hasAccessToVideo`', () => {
        it('should throw 401 when user not authorized', function *() {
            const accessControl = new AccessControl();
            const error = yield catchErrorGenerator(accessControl.hasAccessToVideo.bind(accessControl));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should throw 403 when user not admin or support or assessor', function *() {
            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });
            const error = yield catchErrorGenerator(accessControl.hasAccessToVideo.bind(accessControl));

            expect(error.message).to.equal('User has no access to revise video');
            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_NRV' });
        });

        it('should success for admin', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'admin' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAccessToVideo();
        });

        it('should success for support', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'support' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAccessToVideo();
        });

        it('should success for assessor', function *() {
            const admin = { uid: 1234567890 };
            const role = { code: 'assessor' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const accessControl = new AccessControl({ user: { uid: { value: '1234567890' } } });

            yield accessControl.hasAccessToVideo();
        });
    });
});
