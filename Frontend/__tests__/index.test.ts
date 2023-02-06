import { cn } from '../index';

describe('@bem-react/classname-modules', () => {
    test('cn exported', () => {
        expect(Boolean(cn)).toBe(true);
    });
});
