require('co-mocha');

const { expect } = require('chai');

const testBase = require('tests/models/draw/types/test-base');
const Model = require('models/draw/types/old-eur');
const certData = {
    service: 'direct',
    firstname: 'Наташа',
    lastname: 'Константинопольская',
    certId: 555555,
    confirmedDate: '2015-06-01T12:46:30.135Z',
    dueDate: '2016-06-01T12:46:30.135Z',
    language: 'ru',
    type: 'cert'
};

describe('Draw `old-eur` Model', () => {
    testBase(Model, certData);

    describe('`isSuitable`', () => {
        it('should return true if type is cert and service is "direct"', () => {
            const certModel = new Model(certData, 'png');

            expect(certModel.isSuitable).to.be.true;
        });

        it('should return false if type is not cert', () => {
            const data = Object.assign({}, certData, {
                type: 'unknown_type'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return false if service is "direct_cn"', () => {
            const data = Object.assign({}, certData, {
                service: 'direct_cn'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return false if finished > 15.02.2016', () => {
            const data = Object.assign({}, certData, {
                confirmedDate: '2016-06-01T12:46:30.135Z'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });
    });
});
