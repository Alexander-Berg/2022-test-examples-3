require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');

const dbHelper = require('tests/helpers/clear');
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const { convert } = require('tests/helpers/dateHelper');

const MdsModel = require('models/mds');

const certificateFactory = require('tests/factory/certificatesFactory');
const usersFactory = require('tests/factory/usersFactory');

describe('Certificate findAll controller', () => {
    const user = {
        id: 23,
        uid: 1234567890,
        firstname: 'Petya',
        lastname: 'Petrov'
    };
    const authType = { id: 2, code: 'web' };
    const trialTemplate = { id: 3, previewImagePath: 'path/to/preview/image', slug: 'direct' };
    const finished = new Date(2016, 6, 11);
    const service = {
        id: 12,
        code: 'direct',
        title: 'Yandex.Direct'
    };
    const type = { id: 2, code: 'test' };
    const certificate = {
        id: 13,
        dueDate: new Date(2017, 6, 11),
        firstname: 'Vasya',
        lastname: 'Pupkin',
        active: 1,
        confirmedDate: finished,
        imagePath: '255/38472434872_13'
    };

    before(() => {
        nockBlackbox({});
    });

    beforeEach(dbHelper.clear);

    after(nock.cleanAll);

    it('should return correct fields when user has not certificates', function *() {
        yield usersFactory.createWithRelations(user, { authType });

        const res = yield request
            .get('/v1/certificates/7bX4uf9cpA')
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.firstname).to.equal('Petya');
        expect(actual.lastname).to.equal('Petrov');
        expect(actual.certificates.length).to.equal(0);
    });

    it('should return correct fields when user has certificates', function *() {
        const trial = {
            id: 2,
            finished,
            passed: 1,
            nullified: 0
        };

        yield certificateFactory.createWithRelations(
            certificate,
            { trial, user, trialTemplate, service, type, authType }
        );

        const res = yield request
            .get('/v1/certificates/7bX4uf9cpA')
            .expect(200)
            .end();

        const actual = res.body;

        const expectedCertificates = [
            {
                certId: 13,
                certType: 'test',
                dueDate: convert(new Date(2017, 6, 11)),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: convert(finished),
                imagePath: MdsModel.getAvatarsPath('255/38472434872_13'),
                service,
                previewImagePath: MdsModel.getAvatarsPath('path/to/preview/image'),
                exam: { id: 3, slug: 'direct' }
            }
        ];

        expect(actual.firstname).to.equal('Petya');
        expect(actual.lastname).to.equal('Petrov');
        expect(actual.certificates.length).to.equal(1);
        expect(actual.certificates).to.deep.equal(expectedCertificates);
    });

    it('should not return certificate for nullified trial', function *() {
        const trial = {
            id: 2,
            finished,
            passed: 1,
            nullified: 1
        };

        yield certificateFactory.createWithRelations(
            certificate,
            { trial, user, trialTemplate, service, authType }
        );

        const res = yield request
            .get('/v1/certificates/7bX4uf9cpA')
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.firstname).to.equal('Petya');
        expect(actual.lastname).to.equal('Petrov');
        expect(actual.certificates).to.deep.equal([]);
    });

    it('should throw 400 when `uid` contains invalid characters', function *() {
        yield request
            .get('/v1/certificates/A7bX4uf8hpxA')
            .expect(400)
            .expect({
                message: 'User id contains invalid characters',
                internalCode: '400_UIC'
            })
            .end();
    });

    it('should return 404 when user does not exist', function *() {
        yield request
            .get('/v1/certificates/7777777777777777')
            .expect(404)
            .expect({
                message: 'User not found',
                internalCode: '404_UNF'
            })
            .end();
    });
});
