import { parseFlags } from '../../../shared/utils/flags';

describe('Flags tests', () => {
    describe('#parseFlags', () => {
        it('config=testing;internal=1', () => {
            expect(parseFlags('config=testing;internal=1')).toEqual({
                config: 'testing',
                internal: '1',
            });
        });

        it('foo;;bar', () => {
            expect(parseFlags('foo;;bar')).toEqual({
                foo: '',
                bar: '',
            });
        });

        it('Should return {} when flags is empty', () => {
            expect(parseFlags('')).toEqual({});
        });
    });
});
