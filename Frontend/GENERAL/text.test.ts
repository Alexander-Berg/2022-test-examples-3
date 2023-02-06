import { assert } from 'chai';

import { getCroppedTextWithEndProposal } from './text';

describe('Проверка ката текста', () => {
    describe('Не кропается', () => {
        it('Небольшой текст', () => {
            const exampleText = 'Полтора ящика апельсинов.';
            const result = getCroppedTextWithEndProposal(exampleText, 100);

            assert.equal(result, exampleText);
        });

        it('Длина обрезки попадает в конец предложения', () => {
            const exampleText = 'Полтора ящика апельсинов.';
            const expected = 'Полтора ящика апельсинов.';
            const result = getCroppedTextWithEndProposal(exampleText, 24);

            assert.equal(result, expected);
        });

        it('Предложение без точки', () => {
            const exampleText = 'Полтора ящика апельсинов';
            const expected = 'Полтора ящика апельсинов';
            const result = getCroppedTextWithEndProposal(exampleText, 7);

            assert.equal(result, expected);
        });
    });

    describe('Кропается', () => {
        it('Длина обрезки попадает на середину первого предложения', () => {
            const exampleText = 'Полтора ящика апельсинов. Еще одно предложение';
            const expected = 'Полтора ящика апельсинов.';
            const result = getCroppedTextWithEndProposal(exampleText, 7);

            assert.equal(result, expected);
        });

        it('в 0 символов', () => {
            const exampleText = 'Полтора ящика апельсинов. Еще одно предложение';
            const expected = 'Полтора ящика апельсинов.';
            const result = getCroppedTextWithEndProposal(exampleText, 0);

            assert.equal(result, expected);
        });
    });
});
