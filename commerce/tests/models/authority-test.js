const { expect } = require('chai');

const Authority = require('models/authority');

const AdminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const AdminsFactory = require('tests/factory/adminsFactory');
const dbHelper = require('tests/helpers/clear');

describe('Authority model', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    describe('`find`', () => {
        const admin = { uid: '1029384756' };

        it('`isDeveloper` should return `true` when user is developer', function *() {
            const role = { code: 'developer', active: 1 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isDeveloper).to.be.true;
            expect(authority.isAnalyst).to.be.false;
            expect(authority.isAdmin).to.be.false;
            expect(authority.isAssessor).to.be.false;
            expect(authority.isEditor).to.be.false;
            expect(authority.isSupport).to.be.false;
        });

        it('`isDeveloper` should return `false` when user is developer and role inactive', function *() {
            const role = { code: 'developer', active: 0 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isDeveloper).to.be.false;
        });

        it('`isAnalyst` should return `true` when user is analyst', function *() {
            const role = { code: 'analyst', active: 1 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isAnalyst).to.be.true;
            expect(authority.isDeveloper).to.be.false;
            expect(authority.isAdmin).to.be.false;
            expect(authority.isAssessor).to.be.false;
            expect(authority.isEditor).to.be.false;
            expect(authority.isSupport).to.be.false;
        });

        it('`isAdmin` should return `true` when user is admin', function *() {
            const role = { code: 'admin', active: 1 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isAdmin).to.be.true;
            expect(authority.isDeveloper).to.be.false;
            expect(authority.isAnalyst).to.be.false;
            expect(authority.isAssessor).to.be.false;
            expect(authority.isEditor).to.be.false;
            expect(authority.isSupport).to.be.false;
        });

        it('`isAssessor` should return `true` when user is assessor', function *() {
            const role = { code: 'assessor', active: 1 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isAssessor).to.be.true;
            expect(authority.isDeveloper).to.be.false;
            expect(authority.isAnalyst).to.be.false;
            expect(authority.isAdmin).to.be.false;
            expect(authority.isEditor).to.be.false;
            expect(authority.isSupport).to.be.false;
        });

        it('`isEditor` should return `true` when user is editor', function *() {
            const role = { code: 'editor', active: 1 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isEditor).to.be.true;
            expect(authority.isAssessor).to.be.false;
            expect(authority.isDeveloper).to.be.false;
            expect(authority.isAnalyst).to.be.false;
            expect(authority.isAdmin).to.be.false;
            expect(authority.isSupport).to.be.false;
        });

        it('`isSupport` should return `true` when user is support', function *() {
            const role = { code: 'support', active: 1 };

            yield AdminsToRolesFactory.createWithRelations({}, { admin, role });

            const authority = yield Authority.find('1029384756');

            expect(authority.isSupport).to.be.true;
            expect(authority.isEditor).to.be.false;
            expect(authority.isAssessor).to.be.false;
            expect(authority.isDeveloper).to.be.false;
            expect(authority.isAnalyst).to.be.false;
            expect(authority.isAdmin).to.be.false;

        });

        it('should not have any roles for simple user', function *() {
            yield AdminsFactory.create(admin);

            const authority = yield Authority.find('1029384756');

            expect(authority.isDeveloper).to.be.false;
            expect(authority.isAnalyst).to.be.false;
            expect(authority.isAdmin).to.be.false;
            expect(authority.isAssessor).to.be.false;
            expect(authority.isEditor).to.be.false;
            expect(authority.isSupport).to.be.false;
        });

        it('should have some roles for multi user', function *() {
            yield AdminsToRolesFactory.createWithRelations({}, {
                admin, role: { code: 'developer', active: 1 }
            });
            yield AdminsToRolesFactory.createWithRelations({}, {
                admin, role: { code: 'analyst', active: 1 }
            });
            yield AdminsToRolesFactory.createWithRelations({}, {
                admin, role: { code: 'admin', active: 1 }
            });

            const authority = yield Authority.find('1029384756');

            expect(authority.isDeveloper).to.be.true;
            expect(authority.isAnalyst).to.be.true;
            expect(authority.isAdmin).to.be.true;
        });
    });

    describe('`getRoles`', () => {
        const admin = { id: 123, uid: '1029384756' };

        it('should return correct user roles', function *() {
            yield AdminsToRolesFactory.createWithRelations({}, {
                admin, role: { code: 'developer', active: 1 }
            });
            yield AdminsToRolesFactory.createWithRelations({}, {
                admin, role: { code: 'analyst', active: 1 }
            });
            yield AdminsToRolesFactory.createWithRelations({}, {
                admin, role: { code: 'admin', active: 0 }
            });

            const authority = yield Authority.find('1029384756');
            const roles = authority.getRoles();

            expect(roles).to.have.lengthOf(2);
            expect(roles).to.include('analyst');
            expect(roles).to.include('developer');
        });

        it('should return `[]` when user does not have any role', function *() {
            yield AdminsFactory.create(admin);

            const authority = yield Authority.find('1029384756');
            const roles = authority.getRoles();

            expect(roles).to.deep.equal([]);
        });
    });
});
