import { isFieldVisible } from '../_WithVisibility/FormLayout_WithVisibility';

describe('Form2', () => {
    describe('isFieldVisible', () => {
        it('Поле должно быть видимо по условию ИЛИ', () => {
            expect(
                isFieldVisible(
                    [
                        { foo: '1' },
                        { foo: '2' },
                    ],
                    {
                        foo: { value: '1' },
                    }
                )
            ).toEqual(true);
        });

        it('Поле не должно быть видимо по условию ИЛИ', () => {
            expect(
                isFieldVisible(
                    [
                        { foo: '1' },
                        { foo: '2' },
                    ],
                    {
                        foo: { value: '3' },
                    }
                )
            ).toEqual(false);
        });

        it('Поле должно быть видимо по условию И', () => {
            expect(
                isFieldVisible(
                    [
                        { foo: '1', bar: '1' },
                    ],
                    {
                        foo: { value: '1' },
                        bar: { value: '1' },
                    }
                )
            ).toEqual(true);
        });

        it('Поле не должно быть видимо по условию И', () => {
            expect(
                isFieldVisible(
                    [
                        { foo: '1', bar: '1' },
                    ],
                    {
                        foo: { value: '1' },
                        bar: { value: '2' },
                    }
                )
            ).toEqual(false);
        });
    });
});
