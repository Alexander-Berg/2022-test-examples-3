import { buildCutContent, getCutContent } from './cut';

describe('cut', () => {
    it('content from cut', () => {
        expect(getCutContent('<{myCut\n someContent}>', 'myCut')).toEqual(' someContent');
    });

    it('content from not exists cut', () => {
        expect(getCutContent('<{myCut\n someContent}>', 'myCut2')).toEqual('');
    });

    it('unclosed cut', () => {
        expect(getCutContent('<{myCut\n someContent', 'myCut')).toEqual('');
    });

    it('build cut', () => {
        expect(buildCutContent('someContent', 'myCut')).toEqual('<{myCut\nsomeContent}>');
    });
});
