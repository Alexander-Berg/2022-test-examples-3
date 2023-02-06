require('co-mocha');

const { expect } = require('chai');

const testDrawType = function (Model, certData) {
    describe('`drawCert`', () => {
        it('should return buffer when format png', () => {
            const certModel = new Model(certData, 'png');
            const result = certModel.drawCert();

            expect(result.buffer).to.not.be.empty;
            expect(result.buffer).to.be.an.instanceof(Buffer);
        });

        it('should return buffer when format pdf', () => {
            const certModel = new Model(certData, 'pdf');
            const result = certModel.drawCert();

            expect(result.buffer).to.not.be.empty;
            expect(result.buffer).to.be.an.instanceof(Buffer);
        });
    });
};

module.exports = testDrawType;
