require('co-mocha');

const { expect } = require('chai');

const DrawBase = require('models/draw/base');

describe('Draw base model', () => {
    it('should return correct language', () => {
        const actual = new DrawBase({ language: 'ru' }, 'png');

        expect(actual.language).to.be.equal('ru');
    });

    describe('`isCertificate`', () => {
        it('should return true if can draw certificate for this service', () => {
            const actual = new DrawBase({ type: 'cert' }, 'png');

            expect(actual.isCertificate).to.be.true;
        });

        it('should return false if can`t draw certificate for this service', () => {
            const actual = new DrawBase({ type: 'unknown_type' }, 'png');

            expect(actual.isCertificate).to.be.false;
        });
    });

    describe('`name`', () => {
        it('should return correct name', () => {
            const data = {
                firstname: 'Наташа',
                lastname: 'Константинопольская'
            };

            const actual = new DrawBase(data, 'png');

            expect(actual.name).to.be.equal('Наташа Константинопольская');
        });

        it('should not contain spaces if name is not specified', () => {
            const data = {
                firstname: '',
                lastname: 'Константинопольская'
            };

            const actual = new DrawBase(data, 'png');

            expect(actual.name).to.be.equal('Константинопольская');
        });
    });
});
