require('co-mocha');

const { expect } = require('chai');

const testBase = require('tests/models/draw/types/test-base');
const Model = require('models/draw/types/level-cert');
const InitialsBase = require('models/draw/initials-base');

const templ = {
    firstname: 'Наташа',
    lastname: 'Константинопольская',
    certId: 555555,
    confirmedDate: '2015-06-01T12:46:30.135Z',
    dueDate: '2016-06-01T12:46:30.135Z',
    language: 'ru',
    type: 'cert',
    service: ''
};

const { russianService } = new InitialsBase(templ);

const certDataTempl = { ...templ, service: russianService[0] };

const certData = russianService.map(service => {
    return { ...certDataTempl, service };
});

describe('Draw `level-cert` Model', () => {
    certData.forEach(_certData => {
        testBase(Model, _certData);
    });

    describe('`isSuitable`', () => {
        it('should return true if type is cert and service is russianService', () => {
            certData.forEach(_certData => {
                const certModel = new Model(_certData, 'png');

                expect(certModel.isSuitable).to.be.true;
            });
        });

        it('should return false if type is not cert', () => {
            const data = Object.assign({}, certDataTempl, {
                type: 'unknown_type'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return false if service is "direct_cn"', () => {
            const data = Object.assign({}, certDataTempl, {
                service: 'direct'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return true if finished > 15.02.2016', () => {
            const data = Object.assign({}, certDataTempl, {
                confirmedDate: '2016-06-01T12:46:30.135Z'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.true;
        });
    });
});
