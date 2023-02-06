import { Shape } from '../index';

interface Shape1 {
    a: boolean;
    b?: number;
    c: string;
}

interface Shape2 {
    d: Shape1;
}

interface Shape3 {
    d: 'test' | 'test2';
}

interface Shape4 {
    any: any;
}

describe('Shape', () => {
    it('Basic checks', () => {
        const shape1 = Shape.create<Shape1>({
            a: Shape.is.boolean,
            b: Shape.is.number.optional,
            c: Shape.is.string,
        });

        expect(shape1.validate({ a: true, b: 2, c: 'test' })).toBeTruthy();
        expect(shape1.validate({ a: true, b: 2, c: '' })).toBeTruthy();
        expect(shape1.validate({ a: true, b: undefined, c: 'test' })).toBeTruthy();
        expect(shape1.validate({ a: true, c: 'test' })).toBeTruthy();
        expect(shape1.validate({})).toBeFalsy();
        expect(shape1.validate(2)).toBeFalsy();
        expect(shape1.validate({ a: true })).toBeFalsy();
        expect(shape1.validate({ a: 'string', c: 'string' })).toBeFalsy();
        expect(shape1.validate(undefined)).toBeFalsy();
        expect(shape1.validate(null)).toBeFalsy();
    });

    it('Not empty string', () => {
        const shape1 = Shape.create<Shape1>({
            a: Shape.is.boolean,
            b: Shape.is.number.optional,
            c: Shape.is.notEmptyString,
        });

        expect(shape1.validate({ a: true, b: 2, c: 'test' })).toBeTruthy();
        expect(shape1.validate({ a: true, b: 2, c: '' })).toBeFalsy();
        expect(shape1.validate({ a: true, b: undefined, c: 'test' })).toBeTruthy();
        expect(shape1.validate({ a: true, c: 'test' })).toBeTruthy();
        expect(shape1.validate({})).toBeFalsy();
        expect(shape1.validate(2)).toBeFalsy();
        expect(shape1.validate({ a: true })).toBeFalsy();
        expect(shape1.validate({ a: 'string', c: 'string' })).toBeFalsy();
        expect(shape1.validate(undefined)).toBeFalsy();
        expect(shape1.validate(null)).toBeFalsy();
    });

    it('check shapes', () => {
        const shape1 = Shape.create<Shape1>({
            a: Shape.is.boolean,
            b: Shape.is.optional.number.optional,
            c: Shape.is.string,
        });

        const shape2 = Shape.create<Shape2>({
            d: Shape.is.shape(shape1),
        });

        expect(shape2.validate({ d: { a: true, b: 2, c: 'test' } })).toBeTruthy();
        expect(shape2.validate({ d: { a: true, b: undefined, c: 'test' } })).toBeTruthy();
        expect(shape2.validate({ d: { a: true, c: 'test' } })).toBeTruthy();
        expect(shape2.validate({ d: { a: true } })).toBeFalsy();
        expect(shape2.validate({ d: undefined })).toBeFalsy();
        expect(shape2.validate({ d: null })).toBeFalsy();
    });

    it('check values', () => {
        const shape1 = Shape.create<Shape3>({
            d: Shape.is.values<Shape3['d'][]>('test', 'test2'),
        });

        expect(shape1.validate({ d: 'test' })).toBeTruthy();
        expect(shape1.validate({ d: 'test2' })).toBeTruthy();
        expect(shape1.validate({ d: { a: true } })).toBeFalsy();
        expect(shape1.validate({ d: undefined })).toBeFalsy();
        expect(shape1.validate({ d: null })).toBeFalsy();
        expect(shape1.validate({ d: 'asf' })).toBeFalsy();
        expect(shape1.validate({ d: 3 })).toBeFalsy();
    });

    it('check any', () => {
        const shape1 = Shape.create<Shape4>({
            any: Shape.is.any,
        });

        expect(shape1.validate({ any: 'test2' })).toBeTruthy();
        expect(shape1.validate({ any: { a: true } })).toBeTruthy();
        expect(shape1.validate({ any: undefined })).toBeTruthy();
        expect(shape1.validate({ any: null })).toBeTruthy();
        expect(shape1.validate({ any: 'asf' })).toBeTruthy();
        expect(shape1.validate({ any: 3 })).toBeTruthy();
    });

    it('undefinable should be in object', () => {
        const shape1 = Shape.create({
            und: Shape.is.string.undefinable,
        });

        expect(shape1.validate({ und: 'test2' })).toBeTruthy();
        expect(shape1.validate({ und: undefined })).toBeTruthy();
        expect(shape1.validate({})).toBeFalsy();
    });

    it('optional could not be in object', () => {
        const shape1 = Shape.create({
            und: Shape.is.string.optional,
        });

        expect(shape1.validate({ und: 'test2' })).toBeTruthy();
        expect(shape1.validate({ und: undefined })).toBeTruthy();
        expect(shape1.validate({})).toBeTruthy();
    });
});
