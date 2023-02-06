import { findPhone, findMail } from '../parseFeedback';

describe('turbojson parser common', () => {
    describe('findPhone', () => {
        it('должен вернуть номер телефона', () => {
            expect(findPhone({
                feedback: {
                    stick: 'bottom',
                    buttons: [
                        { type: 'chat' },
                        { type: 'call', url: 'tel:8-800-000-00-01' },
                        { type: 'any' },
                    ],
                },
            })).toBe('8-800-000-00-01');

            expect(findPhone({
                feedback: {
                    stick: 'bottom',
                    buttons: [
                        { type: 'call', url: '8-800-000-00-02' },
                        { type: 'chat' },
                        { type: 'call', url: 'tel:8-800-000-00-01' },
                        { type: 'any' },
                    ],
                },
            })).toBe('8-800-000-00-02');
        });

        it('должен вернуть пустую строку', () => {
            expect(findPhone({
                feedback: {
                    stick: 'bottom',
                    buttons: [],
                },
            })).toBe('');
        });
    });

    describe('findMail', () => {
        it('должен вернуть адресс почты', () => {
            expect(findMail({
                feedback: {
                    stick: 'bottom',
                    buttons: [
                        { type: 'chat' },
                        { type: 'mail', url: 'mailto:example@ya.ru' },
                        { type: 'any' },
                    ],
                },
            })).toBe('example@ya.ru');
            expect(findMail({
                feedback: {
                    stick: 'bottom',
                    buttons: [
                        { type: 'mail', url: 'example2@ya.ru' },
                        { type: 'chat' },
                        { type: 'mail', url: 'mailto:example@ya.ru' },
                        { type: 'any' },
                    ],
                },
            })).toBe('example2@ya.ru');
        });
        it('должен вернуть пустую строку', () => {
            expect(findMail({
                feedback: {
                    stick: 'bottom',
                    buttons: [],
                },
            })).toBe('');
        });
    });
});
