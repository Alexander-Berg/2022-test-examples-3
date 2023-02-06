import { block, classname } from '../bem';

block.prefix = 'my-';

describe('bem', () => {
    it('должен вернуть название блока', () => {
        const actual = block('block')();

        expect(actual).toBe('my-block');
    });

    it('должен вернуть элемент блока', () => {
        const actual = block('block')('elem');

        expect(actual).toBe('my-block__elem');
    });

    it('должен вернуть блок с модификаторами', () => {
        const actual = block('block')({
            a: 'x',
            b: '',
            c: 1,
            d: 0,
            e: true,
            f: undefined,
            g: false,
            h: null,
        });

        expect(actual).toBe('my-block my-block_a_x my-block_c_1 my-block_d_0 my-block_e');
    });

    it('должен вернуть CSS-класс для блока с модификатором', () => {
        const actual = classname('block', { a: 'b' });

        expect(actual).toBe('my-block_a_b');
    });

    it('должен вернуть CSS-класс для элемента с модификатором', () => {
        const actual = classname('block', 'elem', { a: 'b' });

        expect(actual).toBe('my-block__elem_a_b');
    });
});
