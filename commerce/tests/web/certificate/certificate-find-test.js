require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const MdsModel = require('models/mds');

const certificateFactory = require('tests/factory/certificatesFactory');

describe('Certificate find controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    it('should return certificate', function *() {
        const dueDate = new Date(2027, 1, 2, 3, 4, 5);
        const confirmedDate = new Date(2016, 1, 2, 3, 4, 5);
        const confirmedDateTimezoneOffset = confirmedDate.getTimezoneOffset() * 60 * 1000;
        const service = { id: 3, code: 'direct', title: 'Yandex.Direct' };
        const type = { id: 1, code: 'cert' };
        const certData = {
            id: 2,
            firstname: 'Ivan',
            lastname: 'Ivanov',
            dueDate: new Date(dueDate - (dueDate.getTimezoneOffset() * 60 * 1000)),
            active: 1,
            confirmedDate: new Date(confirmedDate - confirmedDateTimezoneOffset),
            imagePath: '603/5674892_2'
        };

        yield certificateFactory.createWithRelations(certData, { service, type });

        const res = yield request
            .post('/v1/certificate/find')
            .send({ certId: 2, lastname: 'Ivanov' })
            .expect(200)
            .end();
        const actual = res.body;

        expect(actual.certId).to.equal(2);
        expect(actual.certType).to.equal('cert');
        expect(actual.dueDate).to.deep.equal('2027-02-02T03:04:05.000Z');
        expect(actual.firstname).to.equal('Ivan');
        expect(actual.lastname).to.equal('Ivanov');
        expect(actual.active).to.equal(1);
        expect(actual.confirmedDate).to.deep.equal('2016-02-02T03:04:05.000Z');
        expect(actual.service).to.deep.equal(service);
        expect(actual.imagePath).to.equal(MdsModel.getAvatarsPath('603/5674892_2'));
    });

    it('should return 400 when certificate id is invalid', function *() {
        yield request
            .post('/v1/certificate/find')
            .send({ certId: 'invalid', lastname: 'Ivanov' })
            .expect(400)
            .expect({
                message: 'Certificate id is invalid',
                internalCode: '400_CII',
                certId: 'invalid'
            })
            .end();
    });

    it('should return 400 when lastname is empty', function *() {
        yield request
            .post('/v1/certificate/find')
            .send({ certId: 2, lastname: '' })
            .expect(400)
            .expect({
                message: 'Lastname is empty',
                internalCode: '400_LNE'
            })
            .end();
    });

    it('should return 400 when lastname contains invalid characters', function *() {
        yield request
            .post('/v1/certificate/find')
            .send({ certId: 2, lastname: 'drop database;' })
            .expect(400)
            .expect({
                message: 'Lastname contains invalid characters',
                internalCode: '400_LIC'
            })
            .end();
    });

    it('should return 404 when certificate not found', function *() {
        yield request
            .post('/v1/certificate/find')
            .send({ certId: 3, lastname: 'Petrov' })
            .expect(404)
            .expect({
                message: 'Certificate not found',
                internalCode: '404_CNF'
            })
            .end();
    });

    it('should return 404 when user find not `cert`', function *() {
        const type = { id: 2, code: 'test' };
        const certData = { id: 3, lastname: 'Petrov' };

        yield certificateFactory.createWithRelations(certData, { type });

        yield request
            .post('/v1/certificate/find')
            .send({ certId: 3, lastname: 'Petrov' })
            .expect(404)
            .expect({
                message: 'Certificate not found',
                internalCode: '404_CNF'
            })
            .end();
    });
});
