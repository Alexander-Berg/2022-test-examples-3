const { expect } = require('chai');
const _ = require('lodash');

const rolesFactory = require('tests/factory/rolesFactory');
const adminsFactory = require('tests/factory/adminsFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');

const dbHelper = require('tests/helpers/clear');
const Idm = require('models/idm');
const { AdminToRole } = require('db/postgres');
const catchError = require('tests/helpers/catchError').generator;

describe('Idm model', () => {
    beforeEach(dbHelper.clear);

    describe('`getActiveRoles`', () => {
        it('should return active roles', function *() {
            yield [
                { code: 'admin', title: 'Administrator', active: 1 },
                { code: 'developer', title: 'Developer', active: 1 },
                { code: 'analyst', title: 'Analyst', active: 1 },
                { code: 'tester', title: 'Tester', active: 0 }
            ].map(rolesFactory.create);

            const actual = yield Idm.getActiveRoles();

            expect(actual).to.deep.equal({
                admin: 'Administrator',
                developer: 'Developer',
                analyst: 'Analyst'
            });
        });

        it('should return empty object when roles is absents', function *() {
            const actual = yield Idm.getActiveRoles();

            expect(actual).to.deep.equal({});
        });
    });

    describe('`getUsers`', () => {
        it('should return all roles', function *() {
            const admin = { id: 1, login: 'mokosha' };

            yield [
                { code: 'developer', title: 'Developer', active: 1 },
                { code: 'tester', title: 'Tester', active: 0 },
                { code: 'analyst', title: 'Analyst', active: 1 }
            ].map(role => adminsToRolesFactory.createWithRelations({}, { admin, role }));

            const actual = yield Idm.getUsers();

            actual[0].roles = _.sortBy(actual[0].roles, 'expert');

            const expected = [
                {
                    login: 'mokosha',
                    roles: [
                        { expert: 'analyst' },
                        { expert: 'developer' }
                    ]
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return all users', function *() {
            const role = { code: 'developer', title: 'Developer' };

            yield [
                { login: 'mokosha' },
                { login: 'mokhov' }
            ].map(admin => adminsToRolesFactory.createWithRelations({}, { admin, role }));

            let actual = yield Idm.getUsers();

            actual = _.sortBy(actual, 'login');

            const expected = [
                {
                    login: 'mokhov',
                    roles: [{ expert: 'developer' }]
                },
                {
                    login: 'mokosha',
                    roles: [{ expert: 'developer' }]
                }

            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return empty array when users not exists', function *() {
            const actual = yield Idm.getUsers();

            expect(actual).to.be.empty;
        });
    });

    describe('`createRole`', () => {
        it('should add new role to admin', function *() {
            yield rolesFactory.create({ id: 1, code: 'analyst' });
            const admin = yield adminsFactory.create({ id: 2 });

            const actual = yield Idm.createRole(admin, 'analyst');

            expect(actual).to.deep.equal({});

            const stored = yield AdminToRole.findAll();

            expect(stored).to.have.length(1);
            expect(stored[0].get('adminId')).to.equal(admin.id);
            expect(stored[0].get('roleId')).to.equal(1);
        });

        it('should success when role already exists', function *() {
            const role = { id: 1, code: 'analyst' };
            const admin = { id: 2 };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const actual = yield Idm.createRole(admin, 'analyst');

            expect(actual).to.deep.equal({ warning: 'Role already exists' });

            const stored = yield AdminToRole.findAll();

            expect(stored).to.have.length(1);
            expect(stored[0].get('adminId')).to.equal(admin.id);
            expect(stored[0].get('roleId')).to.equal(1);
        });

        it('should throw error when role not found', function *() {
            const error = yield catchError(Idm.createRole.bind(Idm, null, 'analyst'));

            expect(error.message).to.equal('Role not found');
            expect(error.status).to.equal(404);
            expect(error.options).to.deep.equal({
                code: 'analyst',
                internalCode: '404_RNF'
            });
        });
    });

    describe('`removeRole`', () => {
        it('should success remove role', function *() {
            const admin = { login: 'akuv' };
            const role = { code: 'admin' };

            yield adminsToRolesFactory.createWithRelations({}, { admin, role });

            const actual = yield Idm.removeRole('akuv', 'admin');

            expect(actual).to.deep.equal({});

            const stored = yield AdminToRole.findAll();

            expect(stored).to.be.empty;
        });

        it('should success when adminToRole not found', function *() {
            yield adminsFactory.create({ login: 'akuv' });
            yield rolesFactory.create({ code: 'admin' });

            const actual = yield Idm.removeRole('akuv', 'admin');

            expect(actual).to.deep.equal({ warning: 'Role already removed' });
        });

        it('should throw error when role not found', function *() {
            yield adminsFactory.create({ login: 'akuv' });

            const error = yield catchError(Idm.removeRole.bind(Idm, 'akuv', 'admin'));

            expect(error.message).to.equal('Role not found');
            expect(error.status).to.equal(404);
            expect(error.options).to.deep.equal({
                code: 'admin',
                internalCode: '404_RNF'
            });
        });

        it('should throw error when admin not found', function *() {
            const error = yield catchError(Idm.removeRole.bind(Idm, 'akuv'));

            expect(error.message).to.equal('Admin not found');
            expect(error.status).to.equal(404);
            expect(error.options).to.deep.equal({
                login: 'akuv',
                internalCode: '404_ADF'
            });
        });
    });
});
