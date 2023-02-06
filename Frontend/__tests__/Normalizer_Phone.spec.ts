import { NormalizerPhone } from '../_Phone/Normalizer_Phone';

describe('NormalizerPhone', () => {
    describe('Код страны +7', () => {
        it('должен корректно обрабатывать явный ввод +7', () => {
            expect(new NormalizerPhone('+7').normalize('+7 906 742 54 72')).toEqual('+7 906 742-54-72');
        });

        it('не должен применять маску, когда первые символы отличны от +7', () => {
            expect(new NormalizerPhone('+7').normalize('+375 906-742-5-472')).toEqual('+375 906-742-5-472');
        });
    });

    describe('Код страны +375', () => {
        it('должен корректно обрабатывать явный ввод кода страны', () => {
            expect(new NormalizerPhone('+375').normalize('+375 906 742 54 72')).toEqual('+375 906 742-54-72');
        });

        it('не должен применять маску, когда первые символы отличны от +375', () => {
            expect(new NormalizerPhone('+375').normalize('+7 906 742-5-472')).toEqual('+7 906 742-5-472');
        });
    });

    describe('Код страны 8', () => {
        it('должен корректно обрабатывать явный ввод кода страны', () => {
            expect(new NormalizerPhone('8').normalize('88007070070')).toEqual('8 800 707-00-70');
        });

        it('не должен применять маску, когда первые символы отличны от 8', () => {
            expect(new NormalizerPhone('8').normalize('+8 800 707 00 70')).toEqual('+8 800 707 00 70');
        });
    });
});
