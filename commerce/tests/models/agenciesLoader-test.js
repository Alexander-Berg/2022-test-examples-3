require('co-mocha');

const AgenciesLoader = require('models/agenciesLoader');
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const moment = require('moment');

const certificatesFactory = require('tests/factory/certificatesFactory');

describe('AgenciesLoader Model', () => {
    const trialTemplate = { id: 3, title: 'Yandex.Direct', isProctoring: false };
    const user = {
        id: 123,
        uid: 1234567890,
        login: 'user-admin'
    };
    const authType = { id: 1, code: 'web' };
    const role = { id: 2, code: 'admin', title: 'Admin' };
    const agency = { id: 15, login: 'admin-agency', manager: 'pupkin' };
    const now = new Date();
    const userExpectedData = {
        agencyLogin: agency.login,
        managerLogin: agency.manager,
        userLogin: 'user-admin',
        firstname: 'Vasya',
        lastname: 'Pupkin',
        userRole: 'Admin',
        examTitle: 'Yandex.Direct',
        isProctoring: 0,
        certId: '23',
        confirmedDate: moment(now).format('DD.MM.YYYY'),
        dueDate: moment(now).add(2, 'month').format('DD.MM.YYYY')
    };

    beforeEach(function *() {
        yield dbHelper.clear();

        const trial = { id: 12, nullified: 0 };
        const certificate = {
            id: 23,
            active: 1,
            firstname: 'Vasya',
            lastname: 'Pupkin',
            dueDate: moment(now).add(2, 'month'),
            confirmedDate: moment(now)
        };

        yield certificatesFactory.createWithRelations(
            certificate,
            { trialTemplate, user, trial, role, agency, authType }
        );
    });

    describe('`getFlatData`', () => {
        it('should return data only for not nullified trial', function *() {
            const nullifiedTrial = { id: 13, nullified: 1 };
            const certificate = {
                id: 24,
                active: 1,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                dueDate: moment(now).add(3, 'month'),
                confirmedDate: moment(now)
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trialTemplate, user, trial: nullifiedTrial, role, agency, authType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            expect(actual).to.deep.equal([userExpectedData]);
        });

        it('should return data only for certificate with dueDate > now', function *() {
            const trial = { id: 14, nullified: 0 };
            const certificate = {
                id: 25,
                active: 1,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                dueDate: moment(now).subtract(3, 'month'),
                confirmedDate: moment(now).subtract(7, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trialTemplate, user, trial, role, agency, authType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            expect(actual).to.deep.equal([userExpectedData]);
        });

        it('should return data only for active certificate', function *() {
            const trial = { id: 14, nullified: 0 };
            const inactiveCertificate = {
                id: 25,
                active: 0,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                dueDate: moment(now).add(3, 'month'),
                confirmedDate: moment(now).subtract(3, 'month')
            };

            yield certificatesFactory.createWithRelations(
                inactiveCertificate,
                { trialTemplate, user, trial, role, agency, authType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            expect(actual).to.deep.equal([userExpectedData]);
        });

        it('should return correct data when user has several active certificates', function *() {
            const trial = { id: 15, nullified: 0 };
            const certificate = {
                id: 26,
                active: 1,
                firstname: 'Petya',
                lastname: 'Petrov',
                dueDate: moment(now).add(5, 'month'),
                confirmedDate: moment(now)
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trialTemplate, user, trial, role, agency, authType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            const dataForSecondCertificate = {
                agencyLogin: agency.login,
                managerLogin: agency.manager,
                userLogin: 'user-admin',
                firstname: 'Petya',
                lastname: 'Petrov',
                userRole: 'Admin',
                examTitle: 'Yandex.Direct',
                isProctoring: 0,
                certId: '26',
                confirmedDate: moment(now).format('DD.MM.YYYY'),
                dueDate: moment(now).add(5, 'month').format('DD.MM.YYYY')
            };

            expect(actual).to.deep.equal([userExpectedData, dataForSecondCertificate]);
        });

        it('should return correct data for several users and one agency', function *() {
            const otherUser = {
                id: 345,
                uid: 9876543210,
                login: 'user-user'
            };
            const otherUserTrial = { id: 16, nullified: 0 };
            const otherUserRole = { id: 3, code: 'user', title: 'User' };
            const certificate = {
                id: 27,
                active: 1,
                firstname: 'Ivan',
                lastname: 'Ivanov',
                dueDate: moment(now).add(6, 'month'),
                confirmedDate: moment(now)
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trialTemplate, user: otherUser, trial: otherUserTrial, role: otherUserRole, agency, authType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            const otherUserExpectedData = {
                agencyLogin: agency.login,
                managerLogin: agency.manager,
                userLogin: 'user-user',
                firstname: 'Ivan',
                lastname: 'Ivanov',
                userRole: 'User',
                examTitle: 'Yandex.Direct',
                isProctoring: 0,
                certId: '27',
                confirmedDate: moment(now).format('DD.MM.YYYY'),
                dueDate: moment(now).add(6, 'month').format('DD.MM.YYYY')
            };

            expect(actual).to.deep.equal([userExpectedData, otherUserExpectedData]);
        });

        it('should return correct data for users from different agencies', function *() {
            const otherUser = {
                id: 456,
                uid: 9876543210,
                login: 'user-user'
            };
            const otherUserTrial = { id: 17, nullified: 0 };
            const otherUserRole = { id: 3, code: 'user', title: 'User' };
            const certificate = {
                id: 28,
                active: 1,
                firstname: 'Ivan',
                lastname: 'Ivanov',
                dueDate: moment(now).add(6, 'month'),
                confirmedDate: moment(now)
            };
            const otherAgency = { id: 16, login: 'other-agency', manager: 'other-pupkin' };

            yield certificatesFactory.createWithRelations(certificate, {
                trialTemplate,
                user: otherUser,
                authType,
                trial: otherUserTrial,
                role: otherUserRole,
                agency: otherAgency
            });

            const actual = yield AgenciesLoader.getFlatData();

            const otherUserExpectedData = {
                agencyLogin: otherAgency.login,
                managerLogin: otherAgency.manager,
                firstname: 'Ivan',
                lastname: 'Ivanov',
                userLogin: 'user-user',
                userRole: 'User',
                examTitle: 'Yandex.Direct',
                isProctoring: 0,
                certId: '28',
                confirmedDate: moment(now).format('DD.MM.YYYY'),
                dueDate: moment(now).add(6, 'month').format('DD.MM.YYYY')
            };

            expect(actual).to.deep.equal([userExpectedData, otherUserExpectedData]);
        });

        it('should return `-` in `confirmedDate` field when `confirmedDate` is null', function *() {
            const otherUser = {
                id: 456,
                uid: 3748593721,
                login: 'user-user'
            };
            const otherUserTrial = { id: 17, nullified: 0 };
            const certificate = {
                id: 28,
                active: 1,
                dueDate: moment(now).add(6, 'month'),
                confirmedDate: null
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trialTemplate, user: otherUser, trial: otherUserTrial, role, agency, authType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            expect(actual[1].confirmedDate).to.equal('-');
        });

        it('should return data only for `web` users', function *() {
            const otherUser = {
                id: 456,
                uid: 3748593721
            };
            const otherTrial = { id: 14, nullified: 0 };
            const otherAuthType = { id: 2, code: 'telegram' };
            const certificate = {
                id: 25,
                active: 1,
                dueDate: moment(now).add(3, 'month'),
                confirmedDate: moment(now).subtract(3, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trialTemplate, user: otherUser, trial: otherTrial, role, agency, authType: otherAuthType }
            );

            const actual = yield AgenciesLoader.getFlatData();

            expect(actual).to.deep.equal([userExpectedData]);
        });

        describe('with proctoring', () => {
            const proTrialTemplate = { id: 33, title: 'Yandex.Direct.PRO', isProctoring: true };

            it('should return correct data in `isProctoring` field', function *() {
                const proTrial = { id: 333, nullified: 0 };
                const confirmedDate = moment(now);
                const dueDate = moment(now).add(1, 'month');
                const proCertificate = {
                    id: 123,
                    active: 1,
                    firstname: 'Vasya',
                    lastname: 'Pupkin',
                    confirmedDate,
                    dueDate
                };

                yield certificatesFactory.createWithRelations(
                    proCertificate,
                    { trialTemplate: proTrialTemplate, trial: proTrial, user, authType, agency, role }
                );

                const expectedProCertificate = {
                    agencyLogin: agency.login,
                    managerLogin: agency.manager,
                    userLogin: 'user-admin',
                    firstname: 'Vasya',
                    lastname: 'Pupkin',
                    userRole: 'Admin',
                    examTitle: 'Yandex.Direct.PRO',
                    isProctoring: 1,
                    certId: '123',
                    confirmedDate: confirmedDate.format('DD.MM.YYYY'),
                    dueDate: dueDate.format('DD.MM.YYYY')
                };

                const actual = yield AgenciesLoader.getFlatData();

                expect(actual).to.deep.equal([userExpectedData, expectedProCertificate]);
            });
        });
    });
});
