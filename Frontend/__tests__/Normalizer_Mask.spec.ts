import { NormalizerMask } from '../_Mask/Normalizer_Mask';

describe('NormalizerMask', () => {
    it('должен оставить цифры в жадном режиме', () => {
        const mask = new NormalizerMask('0.0');

        expect(mask.normalize('495.12')).toEqual('495.12');
        expect(mask.normalize('4')).toEqual('4');
        expect(mask.normalize('495')).toEqual('495');
        expect(mask.normalize('495aaa')).toEqual('495.');
        expect(mask.normalize('495.')).toEqual('495.');
        expect(mask.normalize('495.1')).toEqual('495.1');
        expect(mask.normalize('495.1aaa')).toEqual('495.1');
        expect(mask.normalize('495.1aaa2')).toEqual('495.1');
        expect(mask.normalize('aaa49bbb5ccc.ddd1eee3fff')).toEqual('.');
    });

    it('должен оставить цифры в ленивом режиме', () => {
        expect(new NormalizerMask('+7 999 999 99 99').normalize('90 90 8584 s--98')).toEqual(
            '+7 909 085 84 98'
        );
    });

    it('должен подставить правильный регистр символов', () => {
        expect(new NormalizerMask('X 999 xx').normalize('а1 23а Аб')).toEqual('А 123 аа');
    });
});
