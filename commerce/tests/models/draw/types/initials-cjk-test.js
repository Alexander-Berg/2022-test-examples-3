require('co-mocha');

const { expect } = require('chai');

const testBase = require('tests/models/draw/types/test-base');
const Model = require(`models/draw/types/initials-cjk`);
const certData = {
    service: 'direct_cn',
    firstname: '璐瑶',
    lastname: '邢',
    certId: 555555,
    confirmedDate: '2016-06-01T12:46:30.135Z',
    dueDate: '2017-06-01T12:46:30.135Z',
    language: 'cn',
    type: 'cert'
};

describe('Draw `initials-cjk` model', () => {
    testBase(Model, certData);

    describe('`isSuitable`', () => {
        it('should return true if type is cert and contain cjk symbols', () => {
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

        it('should return false if name not contain cjk symbols', () => {
            const data = Object.assign({}, certData, {
                firstname: 'Наташа',
                lastname: 'Константинопольская'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return false if confirmedDate < 15.02.2016', () => {
            const data = Object.assign({}, certData, {
                confirmedDate: '2015-06-01T12:46:30.135Z'
            });
            const certModel = new Model(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });
    });
});
