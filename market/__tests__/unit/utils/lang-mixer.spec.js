'use strict';

const mixer = require('../../../utils/lang-mixer');

describe('language mixer:', () => {
    describe('rus to eng: ', () => {
        it('should mix string', () => {
            const inputString = 'Более выгодное предложение на ';
            const outputString = mixer.mix(inputString);

            expect(outputString).not.toBe(inputString);
        });

        it('should mix object', () => {
            const inputObject = {
                moreButtonText: 'Еще предложения',
                moreButtonTooltip: 'Предложения других магазинов',
            };
            const outputObject = mixer.mix(inputObject);

            expect(outputObject).not.toEqual(inputObject);
        });

        it('should mix array', () => {
            const inputArray = ['Еще предложения', 'Предложения других магазинов'];
            const outputArray = mixer.mix(inputArray);

            expect(outputArray).not.toEqual(inputArray);
        });

        it('should return input string', () => {
            const inputString = 'Дыши';
            const outputString = mixer.mix(inputString);

            expect(outputString).toBe(inputString);
        });
    });

    describe('eng to rus: ', () => {
        it('should mix string', () => {
            const inputString = 'Better offer on';
            const outputString = mixer.mix(inputString);

            expect(outputString).not.toBe(inputString);
        });

        it('should mix object', () => {
            const inputObject = {
                moreButtonText: 'More offers',
                moreButtonTooltip: 'Other stores offers',
            };
            const outputObject = mixer.mix(inputObject);

            expect(outputObject).not.toEqual(inputObject);
        });

        it('should mix array', () => {
            const inputArray = ['More offers', 'Other stores offers'];
            const outputArray = mixer.mix(inputArray);

            expect(outputArray).not.toEqual(inputArray);
        });
    });

    describe('100% random chance', () => {
        beforeEach(() => {
            mixer._randomizeChance = 1;
        });

        it('should mix string', () => {
            const inputString = 'Более выгодное предложение на ';
            const outputString = mixer.mix(inputString);

            expect(outputString).not.toBe('Бoлee выгoдноe пpeдлoжeниe на ');
        });
    });

    describe('fallback', () => {
        beforeEach(() => {
            mixer._randomizeChance = 0.1;
        });

        it('should mix with 100% chance if cannot mix usual by way', () => {
            for (let i = 0; i < 1000; i += 1) {
                expect(mixer.mix('Спасибо')).not.toBe('Спасибо');
                expect(mixer.mix('Закрыть')).not.toBe('Закрыть');
            }
        });
    });
});
