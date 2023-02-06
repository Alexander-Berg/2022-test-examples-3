require('co-mocha');

const { expect } = require('chai');

const YaLetterPng = require(`models/draw/types/ya-letter-png`);
const certData = {
    service: 'market',
    firstname: 'Дмитрий',
    lastname: 'Моруз',
    certId: 555555,
    confirmedDate: '2017-09-09T19:00:00.000Z',
    dueDate: '2018-09-09T19:00:00.000Z',
    language: 'ru',
    type: 'cert',
    isProctoring: false
};

describe('Draw `ya-letter-png` model', () => {
    describe('`drawCert`', () => {
        it('should return buffer when format png', () => {
            const certModel = new YaLetterPng(certData, 'png');
            const result = certModel.drawCert();

            expect(result.buffer).to.not.be.empty;
            expect(result.buffer).to.be.an.instanceof(Buffer);
        });
    });

    describe('`isSuitable`', () => {
        it('should return true if type is cert and format is png', () => {
            const certModel = new YaLetterPng(certData, 'png');

            expect(certModel.isSuitable).to.be.true;
        });

        it('should return false when format pdf', () => {
            const certModel = new YaLetterPng(certData, 'pdf');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return false if type is not cert', () => {
            const data = Object.assign({}, certData, {
                type: 'achievement'
            });
            const certModel = new YaLetterPng(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });

        it('should return false if confirmedDate < 18.08.2016', () => {
            const data = Object.assign({}, certData, {
                confirmedDate: '2016-06-01T12:46:30.135Z'
            });
            const certModel = new YaLetterPng(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });
    });
});
