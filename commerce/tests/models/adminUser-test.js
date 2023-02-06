const { expect } = require('chai');
const adminFactory = require('tests/factory/adminsFactory');
const AdminUser = require('models/adminUser');
const { Admin } = require('db/postgres');

const config = require('yandex-config');
const nock = require('nock');

describe('AdminUser model', () => {
    beforeEach(require('tests/helpers/clear').clear);

    afterEach(nock.cleanAll);

    describe('`findOrCreate`', () => {
        it('should return stored user if exists', function *() {
            const admin = {
                id: 1,
                uid: 92837465,
                login: 'anyok'
            };

            yield adminFactory.create(admin);

            const actual = yield AdminUser.findOrCreate('anyok', '127.0.0.1');

            expect(actual).to.deep.equal(admin);
        });

        it('should request to BB and return data from BB', function *() {
            const blackboxUser = {
                users: [
                    { uid: { value: '9283746592837465' } }
                ]
            };

            nock(`http://${config.yandexTeamBlackbox.connection.api}`)
                .get('/blackbox')
                .query(true)
                .reply(200, blackboxUser);

            const actual = yield AdminUser.findOrCreate('anyok', '127.0.0.1');

            expect(actual.uid).to.equal(9283746592837465);
            expect(actual.login).to.equal('anyok');

            const stored = yield Admin.findOne({ login: 'anyok' });

            expect(stored.uid).to.equal(9283746592837465);
            expect(stored.login).to.equal('anyok');
        });

        it('should request to BB and return generate data', function *() {
            const blackboxUser = {
                users: [
                    { uid: { value: '9283746592837465' } }
                ]
            };

            nock(`http://${config.yandexTeamBlackbox.connection.api}`)
                .get('/blackbox')
                .query(true)
                .reply(200, blackboxUser);

            const actual = yield AdminUser.findOrCreate('anyok', '127.0.0.1');

            expect(actual.uid).to.equal(9283746592837465);
            expect(actual.login).to.equal('anyok');
        });
    });
});
