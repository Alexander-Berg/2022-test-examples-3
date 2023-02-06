require('co-mocha');

const config = require('yandex-config');
const { expect } = require('chai');

const InitialsBase = require('models/draw/initials-base');

const certData = {
    service: 'direct'
};

describe('Initials base model', () => {
    it('should return correct logo', () => {
        const actual = new InitialsBase(certData, 'png');
        const expectedImage = config.draw.services.initials.direct.logo.png;

        expect(actual.logo).to.deep.equal(expectedImage);
    });

    it('should return correct background', () => {
        const actual = new InitialsBase(certData, 'png');
        const expectedImage = config.draw.services.initials.direct.background.png;

        expect(actual.background).to.deep.equal(expectedImage);
    });

    describe('`hasCjkSymbols`', () => {
        it('should return true if fullname contains chinese symbols', () => {
            const data = {
                firstname: 'Suizong',
                lastname: '测试'
            };
            const actual = new InitialsBase(Object.assign(certData, data), 'png');

            expect(actual.hasCjkSymbols).to.be.true;
        });

        it('should return false if fullname not contains chinese symbols', () => {
            const data = {
                firstname: 'Наташа',
                lastname: 'Константинопольская'
            };
            const actual = new InitialsBase(Object.assign(certData, data), 'png');

            expect(actual.hasCjkSymbols).to.be.false;
        });
    });

    describe('`offsets`', () => {
        it('should return correct offsets for png', () => {
            const actual = new InitialsBase(certData, 'png');
            const expectedOffsets = {
                left: 60 + 0,
                top: 60 + 0
            };

            expect(actual.offset).to.deep.equal(expectedOffsets);
        });

        it('should return correct offsets for pdf', () => {
            const actual = new InitialsBase(certData, 'pdf');
            const expectedOffsets = {
                left: 174 + 60,
                top: 174 + 295
            };

            expect(actual.offset).to.deep.equal(expectedOffsets);
        });
    });
});
