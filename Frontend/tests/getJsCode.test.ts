import { getJsCode } from '../src/getJsCode';

describe('getJsCode', () => {
    test('result with empty code', () => {
        const code = getJsCode('', '123');

        expect(code).toBe('');
    });

    test('result with nonce', () => {
        const code = getJsCode('console.log("test");', '123');

        expect(code).toBe('<script type="text/javascript" nonce="123">console.log("test");</script>');
    });

    test('result without nonce', () => {
        const code = getJsCode('console.log("test");');

        expect(code).toBe('<script type="text/javascript">console.log("test");</script>');
    });
});
