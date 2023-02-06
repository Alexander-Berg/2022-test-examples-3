import { recognizeColor } from '../ColorPicker.utils';

describe('ColorPicker', () => {
    describe('recognizeColor()', () => {
        it('Жёлтый', () => {
            expect(recognizeColor('Жёлтый')).toEqual({
                color: '#FFCC00',
            });
        });

        it('Тёмно-зелёный', () => {
            expect(recognizeColor('Тёмно-зелёный')).toEqual({
                color: '#418E04',
            });
        });

        it('Серобуромалиновый', () => {
            expect(recognizeColor('Серобуромалиновый')).toBeUndefined();
        });
    });
});
