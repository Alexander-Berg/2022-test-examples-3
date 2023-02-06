import { insertToTemplateString } from '../templateStrings';

describe('utils/insertToTemplateString', () => {
    it('inserts phrase to template string', () => {
        expect(insertToTemplateString('#template# world', 'hello')).toEqual(['', 'hello', ' world']);
    });

    it('returns only phrase if there is no hashtag', () => {
        expect(insertToTemplateString('template world', 'hello')).toEqual(['template world']);
    });
});
