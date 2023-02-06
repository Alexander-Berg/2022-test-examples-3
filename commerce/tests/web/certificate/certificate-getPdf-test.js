require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const nock = require('nock');

const nockPdf = require('tests/helpers/mdsServices').mds.pdf;
const certificateFactory = require('tests/factory/certificatesFactory');

describe('Certificate getPdf controller', () => {
    const user = {
        uid: 1234567890,
        firstname: 'Ivan',
        lastname: 'Ivanov'
    };
    const authType = { code: 'web' };
    const dueDate = new Date(2027, 1, 2, 3, 4, 5);
    const confirmedDate = new Date(2016, 1, 2, 3, 4, 5);
    const service = { id: 3, code: 'direct', title: 'Yandex.Direct' };
    const type = { id: 1, code: 'cert' };
    const certData = {
        id: 2,
        firstname: 'Ivan',
        lastname: 'Ivanov',
        dueDate: new Date(dueDate - (dueDate.getTimezoneOffset() * 60 * 1000)),
        active: 1,
        confirmedDate: new Date(confirmedDate - (confirmedDate.getTimezoneOffset() * 60 * 1000)),
        imagePath: '603/5674892_2',
        pdfPath: '603/5674892_2.pdf'
    };

    beforeEach(dbHelper.clear);

    before(nockPdf);

    after(nock.cleanAll);

    it('should return stream from MDS if pdfPath exists', function *() {
        yield certificateFactory.createWithRelations(certData, { service, type, user, authType });
        const res = yield request
            .get('/v1/certificate/pdf/7bX4uf9cpA/2')
            .set('Accept', 'application/pdf')
            .expect(200)
            .end();

        const actual = res.text;

        expect(actual).to.be.equal('pdf content');
    });

    it('should return buffer if pdfPath not exists', function *() {
        const data = Object.assign({}, certData, { pdfPath: null });

        yield certificateFactory.createWithRelations(data, { service, type, user, authType });

        const res = yield request
            .get('/v1/certificate/pdf/7bX4uf9cpA/2')
            .set('Accept', 'application/pdf')
            .expect(200)
            .end();

        const actual = res.text;

        expect(actual).to.not.be.empty;
    });

    it('should throw 400 when `hashedUserId` contains invalid characters', function *() {
        yield request
            .get('/v1/certificate/pdf/A7bX4uf8hpxA/2')
            .expect(400)
            .expect({
                message: 'User id contains invalid characters',
                internalCode: '400_UIC'
            })
            .end();
    });

    it('should throw 400 when `certId` is invalid', function *() {
        yield request
            .get('/v1/certificate/pdf/7bX4uf9cpA/invalid')
            .expect(400)
            .expect({
                message: 'Certificate id is invalid',
                internalCode: '400_CII',
                certId: 'invalid'
            })
            .end();
    });

    it('should throw 404 when certificate not found', function *() {
        yield request
            .get('/v1/certificate/pdf/7bX4uf9cpA/222')
            .expect(404)
            .expect({
                message: 'Certificate not found',
                internalCode: '404_CNF'
            })
            .end();
    });

    it('should throw 404 when certificate type is not `cert`', function *() {
        yield certificateFactory.createWithRelations(certData, {
            service, type: { id: 2, code: 'achievement' }, user
        });

        yield request
            .get('/v1/certificate/pdf/7bX4uf9cpA/2')
            .expect(404)
            .expect({
                message: 'Certificate not found',
                internalCode: '404_CNF'
            })
            .end();
    });
});
