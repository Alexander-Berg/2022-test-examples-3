require('co-mocha');

const { expect } = require('chai');
const moment = require('moment');

const dbHelper = require('tests/helpers/clear');
const { func: catchErrorFunc } = require('tests/helpers/catchError');

const usersFactory = require('tests/factory/usersFactory');
const rolesFactory = require('tests/factory/rolesFactory');
const authTypesFactory = require('tests/factory/authTypesFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const proctoringVideosFactory = require('tests/factory/proctoringVideosFactory');
const userIdentificationsFactory = require('tests/factory/userIdentificationsFactory');

const baseTests = require('tests/models/user/test-base');

const WebUser = require('models/user/webUser');
const { User } = require('db/postgres');

describe('Web user model', () => {
    beforeEach(dbHelper.clear);

    baseTests(WebUser);

    describe('`findAndRenew`', () => {
        it('should return stored user', function *() {
            const role = { id: 1 };
            const authType = { id: 2, code: 'web' };

            yield usersFactory.createWithRelations({
                id: 123,
                uid: 123456789012345,
                login: 'current-user',
                firstname: 'user-firstname',
                lastname: 'user-lastname'
            }, { role, authType });

            const blackboxUser = {
                uid: { value: '123456789012345' }
            };
            const actual = yield WebUser.findAndRenew(blackboxUser);

            expect(actual.id).to.equal(123);
            expect(actual.get('uid')).to.equal(123456789012345);
            expect(actual.get('yandexUid')).to.equal(123456789012345);
            expect(actual.get('login')).to.equal('current-user');
            expect(actual.get('firstname')).to.equal('user-firstname');
            expect(actual.get('lastname')).to.equal('user-lastname');
            expect(actual.get('roleId')).to.equal(1);
            expect(actual.get('authTypeId')).to.equal(2);
        });

        it('should create new user', function *() {
            yield rolesFactory.create({ id: 1 });
            yield authTypesFactory.create({ id: 3, code: 'web' });
            const blackboxUser = {
                uid: { value: '123456789012345' },
                login: 'new-user',
                attributes: {
                    27: 'user-firstname',
                    28: 'user-lastname'
                }
            };
            const actual = yield WebUser.findAndRenew(blackboxUser);

            expect(actual.get('uid')).to.equal(123456789012345);
            expect(actual.get('yandexUid')).to.equal(123456789012345);
            expect(actual.get('login')).to.equal('new-user');
            expect(actual.get('firstname')).to.equal('user-firstname');
            expect(actual.get('lastname')).to.equal('user-lastname');
            expect(actual.get('roleId')).to.equal(1);
            expect(actual.get('authTypeId')).to.equal(3);
        });

        it('should update user from blackbox', function *() {
            const role = { id: 1 };
            const authType = { id: 2, code: 'web' };

            yield usersFactory.createWithRelations({
                id: 2345,
                uid: 123456789012345,
                login: 'old-login',
                firstname: 'old-firstname',
                lastname: 'old-lastname'
            }, { role, authType });

            const blackboxUser = {
                uid: { value: '123456789012345' },
                login: 'new-user',
                attributes: {
                    27: 'user-firstname',
                    28: 'user-lastname'
                }
            };
            const actual = yield WebUser.findAndRenew(blackboxUser);

            expect(actual.id).to.equal(2345);
            expect(actual.get('uid')).to.equal(123456789012345);
            expect(actual.get('yandexUid')).to.equal(123456789012345);
            expect(actual.get('login')).to.equal('new-user');
            expect(actual.get('firstname')).to.equal('user-firstname');
            expect(actual.get('lastname')).to.equal('user-lastname');
            expect(actual.get('roleId')).to.equal(1);
            expect(actual.get('authTypeId')).to.equal(2);
        });

        it('should store', function *() {
            const role = { id: 1 };
            const authType = { id: 2, code: 'web' };

            yield usersFactory.createWithRelations({
                id: 23,
                uid: 123456789012345,
                login: 'current-user'
            }, { role, authType });

            const blackboxUser = {
                uid: { value: '123456789012345' },
                login: 'new-user'
            };

            yield WebUser.findAndRenew(blackboxUser);
            const actual = yield User.findAll({ where: { uid: 123456789012345, authTypeId: 2 } });

            expect(actual).to.have.length(1);
            expect(actual[0].id).to.equal(23);
            expect(actual[0].get('uid')).to.equal(123456789012345);
            expect(actual[0].get('yandexUid')).to.equal(123456789012345);
            expect(actual[0].get('login')).to.equal('new-user');
        });

        it('should not update `roleId` field when this field is not null', function *() {
            const role = { id: 3 };
            const authType = { id: 2, code: 'web' };

            yield usersFactory.createWithRelations({
                id: 12,
                uid: 123456789012345,
                login: 'current-user'
            }, { role, authType });

            const blackboxUser = {
                uid: { value: '123456789012345' },
                login: 'new-user'
            };

            const actual = yield WebUser.findAndRenew(blackboxUser);

            expect(actual.get('roleId')).to.equal(3);
        });

        it('should not update user with same uid and other authType', function *() {
            yield authTypesFactory.create({ code: 'web' });
            yield rolesFactory.create({ id: 1 });
            yield usersFactory.createWithRelations({
                id: 1234,
                uid: 123456,
                login: 'old-login',
                firstname: 'old-firstname',
                lastname: 'old-lastname'
            }, { authType: { code: 'telegram' } });

            const blackboxUser = {
                uid: { value: '123456789012345' },
                login: 'new-user',
                attributes: {
                    27: 'user-firstname',
                    28: 'user-lastname'
                }
            };

            yield WebUser.findAndRenew(blackboxUser);

            const actual = yield User.findById(1234);

            expect(actual.id).to.equal(1234);
            expect(actual.get('uid')).to.equal(123456);
            expect(actual.get('login')).to.equal('old-login');
            expect(actual.get('firstname')).to.equal('old-firstname');
            expect(actual.get('lastname')).to.equal('old-lastname');
        });
    });

    describe('`getUserInfo`', () => {
        it('should return correct data when user exists in blackbox and has not certificates', () => {
            const blackboxData = {
                users: [
                    {
                        uid: { value: 1029384756 },
                        attributes: {
                            27: 'Ivan',
                            28: 'Ivanov'
                        }
                    }
                ]
            };

            const actual = WebUser.getUserInfo(blackboxData);

            expect(actual.firstname).to.equal('Ivan');
            expect(actual.lastname).to.equal('Ivanov');
        });

        it('should return 404 when user does not exist in blackbox', () => {
            const blackboxData = {
                body: {
                    users: [{ uid: {} }]
                }
            };

            const error = catchErrorFunc(WebUser.getUserInfo.bind(null, blackboxData));

            expect(error.message).to.equal('User not found');
            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF' });
        });
    });

    describe('`getSavedData`', () => {
        it('should return correct user data', function *() {
            const successStarted = new Date(2017, 3, 5);
            const successFinished = new Date(2017, 3, 6);
            const dueDate = moment(successFinished).add(1, 'year').toDate();
            const successTrial = {
                id: 12,
                started: successStarted,
                finished: successFinished,
                passed: 1,
                nullified: 0
            };
            const user = {
                id: 34,
                firstname: 'Okto',
                lastname: 'Kota',
                login: 'dotokoto',
                uid: 123789
            };
            const trialTemplate = { title: 'Сертификация специалистов по&nbsp;Яндекс Кофе' };
            const certificateData = {
                firstname: 'Doto',
                lastname: 'Koto',
                confirmedDate: successFinished,
                dueDate,
                active: 1
            };

            yield certificatesFactory.createWithRelations(
                certificateData,
                { trial: successTrial, user, trialTemplate }
            );

            const failedStarted = new Date(2017, 1, 5);
            const failedFinished = new Date(2017, 1, 6);
            const failedTrial = {
                started: failedStarted,
                finished: failedFinished,
                passed: 0,
                nullified: 1
            };
            const otherTrialTemplate = { title: 'Сертификация&nbsp;по&nbsp;Яндекс Музыке' };

            yield trialsFactory.createWithRelations(
                failedTrial,
                { trial: failedTrial, trialTemplate: otherTrialTemplate, user }
            );

            const { userData } = yield WebUser.getSavedData(123789);

            const expected = {
                login: 'dotokoto',
                firstname: 'Okto',
                lastname: 'Kota',
                attempts: [
                    {
                        started: failedStarted,
                        finished: failedFinished,
                        passed: 0,
                        nullified: 1,
                        exam: 'Сертификация по Яндекс Музыке'
                    },
                    {
                        started: successStarted,
                        finished: successFinished,
                        passed: 1,
                        nullified: 0,
                        exam: 'Сертификация специалистов по Яндекс Кофе',
                        certificate: certificateData
                    }
                ]
            };

            expect(userData).to.deep.equal(expected);
        });

        it('should return correct user links', function *() {
            const user = { id: 7, uid: 12345 };
            const firstTrialTemplate = { id: 11 };
            const secondTrialTemplate = { id: 22 };

            yield proctoringVideosFactory.createWithRelations(
                { name: 'first.webm', startTime: 123, source: 'webcam' },
                { trial: { id: 1 }, user, trialTemplate: firstTrialTemplate }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: 'second.webm', startTime: 456, source: 'webcam' },
                { trial: { id: 1 }, user, trialTemplate: firstTrialTemplate }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: 'third.webm', startTime: 333, source: 'webcam' },
                { trial: { id: 2 }, user, trialTemplate: secondTrialTemplate }
            );

            yield userIdentificationsFactory.createWithRelations(
                { id: 1, face: 'faces/1_face.jpg', document: 'documents/1_doc.jpg' },
                { user, trialTemplate: firstTrialTemplate }
            );
            yield userIdentificationsFactory.createWithRelations(
                { id: 2, face: 'faces/21_face.jpg', document: 'documents/21_doc.jpg' },
                { user, trialTemplate: secondTrialTemplate }
            );
            yield userIdentificationsFactory.createWithRelations(
                { id: 3, face: 'faces/22_face.jpg', document: 'documents/22_doc.jpg' },
                { user, trialTemplate: secondTrialTemplate }
            );

            const { links } = yield WebUser.getSavedData(12345);

            const expected = {
                videos: [
                    'https://test-host.ru/v1/user/takeout/videos/first.webm',
                    'https://test-host.ru/v1/user/takeout/videos/second.webm',
                    'https://test-host.ru/v1/user/takeout/videos/third.webm'
                ],
                faces: [
                    'https://test-host.ru/v1/user/takeout/faces/1_face.jpg',
                    'https://test-host.ru/v1/user/takeout/faces/21_face.jpg',
                    'https://test-host.ru/v1/user/takeout/faces/22_face.jpg'
                ],
                documents: [
                    'https://test-host.ru/v1/user/takeout/documents/1_doc.jpg',
                    'https://test-host.ru/v1/user/takeout/documents/21_doc.jpg',
                    'https://test-host.ru/v1/user/takeout/documents/22_doc.jpg'
                ]
            };

            expect(links).to.deep.equal(expected);
        });

        it('should filter videos by source', function *() {
            const user = { id: 7, uid: 12345 };
            const trial = { id: 1 };
            const trialTemplate = { id: 11 };

            yield proctoringVideosFactory.createWithRelations(
                { name: 'first.webm', startTime: 123, source: 'webcam' },
                { trial, user, trialTemplate }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: 'second.webm', startTime: 456, source: 'screen' },
                { trial, user, trialTemplate }
            );

            const { links } = yield WebUser.getSavedData(12345);

            const expected = {
                videos: ['https://test-host.ru/v1/user/takeout/videos/first.webm'],
                faces: [],
                documents: []
            };

            expect(links).to.deep.equal(expected);
        });

        it('should return `[]` in links when they are absent', function *() {
            yield usersFactory.createWithRelations({ id: 3, uid: 123 });

            const { links } = yield WebUser.getSavedData(123);

            expect(links).to.deep.equal({ videos: [], faces: [], documents: [] });
        });

        it('should return requested user', function *() {
            yield usersFactory.createWithRelations({
                login: 'anyok',
                uid: 777
            }, {});

            yield usersFactory.createWithRelations({
                login: 'coffee',
                uid: 666
            }, {});

            const { userData } = yield WebUser.getSavedData(777);

            expect(userData.login).to.equal('anyok');
        });

        it('should not return empty fields', function *() {
            yield usersFactory.createWithRelations({
                firstname: 'Any',
                lastname: null,
                login: 'anyok',
                uid: 777
            }, {});

            const { userData } = yield WebUser.getSavedData(777);

            expect(userData).to.deep.equal({
                firstname: 'Any',
                login: 'anyok'
            });
        });

        it('should return `null` when user is absent', function *() {
            const actual = yield WebUser.getSavedData(123789);

            expect(actual).to.be.null;
        });
    });

    describe('findByLogins', () => {
        const role = { id: 1 };
        const authType = { id: 2, code: 'web' };

        it('should filter users by logins', function *() {
            yield usersFactory.createWithRelations({
                id: 111,
                uid: 12345,
                login: 'first-user'
            }, { role, authType });
            yield usersFactory.createWithRelations({
                id: 222,
                uid: 22345,
                login: 'second-user'
            }, { role, authType });
            yield usersFactory.createWithRelations({
                id: 333,
                uid: 32345,
                login: 'third-user'
            }, { role, authType });

            const res = yield WebUser.findByLogins({ logins: ['second-user', 'third-user'] });

            expect(res).to.have.length(2);
            expect(res[0].id).to.equal(222);
            expect(res[1].id).to.equal(333);
        });

        it('should filter users by auth type', function *() {
            yield usersFactory.createWithRelations({
                id: 111,
                uid: 12345,
                login: 'first-user'
            }, { role, authType });
            yield usersFactory.createWithRelations({
                id: 222,
                uid: 22345,
                login: 'second-user'
            }, { role, authType: { id: 1, code: 'not-web' } });

            const res = yield WebUser.findByLogins({ logins: ['first-user', 'second-user'] });

            expect(res).to.have.length(1);
            expect(res[0].id).to.equal(111);
        });

        it('should return requested attributes', function *() {
            yield usersFactory.createWithRelations({
                id: 111,
                uid: 12345,
                login: 'first-user'
            }, { role, authType });

            const res = yield WebUser.findByLogins({ logins: ['first-user'], attributes: ['id', 'uid'] });

            expect(res).to.deep.equal([
                { id: 111, uid: 12345 }
            ]);
        });

        it('should return empty array if users does not exists', function *() {
            const res = yield WebUser.findByLogins(['some-user']);

            expect(res).to.deep.equal([]);

        });
    });

    describe('`saveEmail`', () => {
        it('should save email by userId', function *() {
            yield usersFactory.createWithRelations({ id: 123, email: null });
            yield usersFactory.createWithRelations({ id: 456, email: null });

            yield WebUser.saveEmail(123, 'expert@yandex.ru');

            const actual = yield User.findAll({
                attributes: ['id', 'email'],
                order: [['id']],
                raw: true
            });

            expect(actual).to.deep.equal([
                { id: 123, email: 'expert@yandex.ru' },
                { id: 456, email: null }
            ]);
        });

        it('should do nothing when user does not exist', function *() {
            yield WebUser.saveEmail(123, 'expert@yandex.ru');

            const actual = yield User.findAll();

            expect(actual).to.deep.equal([]);
        });
    });
});
