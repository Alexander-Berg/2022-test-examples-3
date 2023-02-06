require('co-mocha');

const { expect } = require('chai');
const DrawModel = require('models/draw');

const certData = type => ({
    service: 'direct',
    firstname: 'Наташа',
    lastname: 'Константинопольская',
    certId: 555555,
    confirmedDate: '2016-06-01T12:46:30.135Z',
    dueDate: '2016-06-01T12:46:30.135Z',
    language: 'ru',
    type
});

describe('Draw model', () => {
    describe('`drawCert`', () => {
        it('should return buffer for correct cert', () => {
            const actual = DrawModel.drawCert(certData('cert'), 'pdf');

            expect(actual.url).to.be.undefined;
            expect(actual.buffer).to.not.be.undefined;
            expect(actual.buffer).to.be.an.instanceof(Buffer);
        });

        it('should return empty string when service is unknown', () => {
            const data = Object.assign(certData('cert'), { service: 'unknown_service' });
            const actual = DrawModel.drawCert(data, 'pdf');

            expect(actual).to.deep.equal({ url: '' });
        });
    });
});
