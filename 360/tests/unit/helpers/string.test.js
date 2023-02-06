import _ from 'lodash';
import string from '../../../components/helpers/string';

describe('stringHelper', () => {
    describe('Метод `capitalize`', () => {
        it('должен вернуть слово с заглавной буквы', () => {
            expect(string.capitalize('привет')).toBe('Привет');
        });

        it('должен вернуть то исходной значение, если исходное значение не строка', () => {
            expect(string.capitalize(123)).toBe(123);
        });
    });

    describe('getUnique', () => {
        const numberTests = 100;

        it('should create ' + numberTests + ' different names from one source', () => {
            const name = 'boogie.png';
            const results = [];
            for (let i = 0; i < numberTests; i++) {
                results.push(string.getUnique(name));
            }

            expect(_.uniq(results).length).toBe(numberTests);
        });
    });

    describe('processHashResource', () => {
        const tests = {
            '34cg wefg9u2rgfgsfdg sfdg sfd': '34cg+wefg9u2rgfgsfdg+sfdg+sfd',
            '34cg wefg9u2r+gfgsfdg sf+dg sfd': '34cg+wefg9u2r+gfgsfdg+sf+dg+sfd',
            '34cg wefg9u2rgfgsfdg:/dfsf dg fgsfdgsfdg': '34cg+wefg9u2rgfgsfdg:/dfsf dg fgsfdgsfdg',
            '34cg wefg9u2rgfgsfdg:/dfsf+dg+fgsfdgsfdg': '34cg+wefg9u2rgfgsfdg:/dfsf+dg+fgsfdgsfdg',
            '34cg wefg9u2rgf+gsf+dg:/dfsfdgfgsfdgsfdg': '34cg+wefg9u2rgf+gsf+dg:/dfsfdgfgsfdgsfdg'
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = string.processHashResource(input);
                expect(result).toBe(output);
            });
        });
    });

    describe('#createId', () => {
        it('должен вернуть число', () => {
            expect(typeof string.createId('hello')).toBe('number');
        });
        it('должен вернуть одинаковый id для одинаковый строк', () => {
            expect(string.createId('world')).toBe(string.createId('world'));
        });
        it('должен вернуть разные id для разных строк', () => {
            expect(string.createId('world')).not.toBe(string.createId('hello'));
        });
    });
});
