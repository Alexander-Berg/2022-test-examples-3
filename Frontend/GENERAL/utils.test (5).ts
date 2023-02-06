import { assert, expect } from 'chai';
import type { ICrocodileGameWord } from '../CrocodileGame.typings/client';
import { getWords } from './utils';

const mockDictionary: ICrocodileGameWord[] = [
    {
        id: 0,
        word: 'Тест',
    },
    {
        id: 1,
        word: 'Юнит',
    },
    {
        id: 2,
        word: 'Функция',
    },
    {
        id: 3,
        word: 'Данные',
    },
    {
        id: 4,
        word: 'Гермиона',
    },
    {
        id: 5,
        word: 'CI',
    },
];

describe('CrocodileGame', () => {
    describe('getWords', () => {
        it('should return words with property "word" with type string', () => {
            assert.typeOf(
                getWords(1, [], mockDictionary)[0].word,
                'string',
            );
        });

        it('should return 5 words', () => {
            assert.lengthOf(getWords(5, [], mockDictionary), 5);
        });

        it('should return word "CI"', () => {
            expect(getWords(1, [0, 1, 2, 3, 4], mockDictionary)[0].word).to.equal('CI');
        });
    });
});
