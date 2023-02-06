const expect = require('chai').expect;
const template = require('./template');

const data = {
    key: 'name',
    value: 'vasya',
};

describe('template.build', () => {
    it('должен подставить параметры из объекта в шаблон строки', () => {
        expect(template.build('${key}=${value}', data)).to.be.equal('name=vasya');
    });
});
