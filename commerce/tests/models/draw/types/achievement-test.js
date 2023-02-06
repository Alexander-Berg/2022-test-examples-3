require('co-mocha');

const { expect } = require('chai');
const { achievements } = require('yandex-config');
const AchievementModel = require(`models/draw/types/achievement`);
const certData = {
    service: 'hello',
    firstname: 'Наташа',
    lastname: 'Константинопольская',
    certId: 555555,
    confirmedDate: '2016-06-01T12:46:30.135Z',
    dueDate: '2016-06-01T12:46:30.135Z',
    language: 'ru',
    type: 'achievement'
};

describe('Draw achievement model', () => {
    describe('`isSuitable`', () => {
        it('should return true if type is achievement', () => {
            const certModel = new AchievementModel(certData, 'png');

            expect(certModel.isSuitable).to.be.true;
        });

        it('should return false if type is not achievement', () => {
            const data = Object.assign({}, certData, {
                type: 'unknown_type'
            });
            const certModel = new AchievementModel(data, 'png');

            expect(certModel.isSuitable).to.be.false;
        });
    });

    describe('`drawCert`', () => {
        it('should return url if achievement exists', () => {
            const data = Object.assign({}, certData, {
                type: 'achievement',
                service: 'hello'
            });
            const certModel = new AchievementModel(data, 'png');
            const actual = certModel.drawCert(data, 'pdf');

            expect(actual.buffer).to.be.undefined;
            expect(actual.url).to.be.equal(achievements.hello);
        });

        it('should return empty string for unknown achievement', () => {
            const data = Object.assign({}, certData, {
                type: 'achievement',
                service: 'unknown_service'
            });
            const certModel = new AchievementModel(data, 'png');
            const actual = certModel.drawCert(data, 'pdf');

            expect(actual).to.deep.equal({ url: '' });
        });
    });
});
