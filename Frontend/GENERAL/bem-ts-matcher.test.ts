import { BemTSMatcher } from './bem-ts-matcher';

describe('BemTSMatcher', () => {
    // eslint-disable-next-line
    let instance:any;

    beforeEach(() => {
        instance = new BemTSMatcher({ src: 'src' });
    });

    describe('matchDefaultImports', () => {
        it('should parse default file imports', () => {
            instance._existsSync = () => true;

            const match = instance.matchDefaultImports('Button/Button');
            expect(match.cell.entity.block).toBe('Button');
        });

        it('should return null if default file does not exist', () => {
            instance._existsSync = () => false;

            const match = instance.matchDefaultImports('Button/Button');
            expect(match).toEqual({});
        });
    });
});
