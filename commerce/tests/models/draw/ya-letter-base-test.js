require('co-mocha');

const config = require('yandex-config');
const { expect } = require('chai');

const YaLetterBaseBase = require('models/draw/ya-letter-base');

const certData = {
    service: 'direct_en',
    language: 'en'
};

describe('YaLetter base model', () => {
    it('should return correct logo', () => {
        const actual = new YaLetterBaseBase(certData, 'pdf');
        const expectedImage = config.draw.services.yaLetter.en.logo.pdf;

        expect(actual.logo).to.deep.equal(expectedImage);
    });

    it('should return correct background', () => {
        const actual = new YaLetterBaseBase(certData, 'png');
        const expectedImage = config.draw.services.yaLetter.en.background.png;

        expect(actual.background).to.deep.equal(expectedImage);
    });

    it('should return correct service', () => {
        const actual = new YaLetterBaseBase(certData, 'png');
        const expectedService = config.draw.tankerKeys.direct_en;

        expect(actual.service).to.deep.equal(expectedService);
    });
});
