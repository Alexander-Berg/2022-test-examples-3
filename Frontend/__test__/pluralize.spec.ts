import { pluralize, pluralizeStr } from '../pluralize';

const s1 = 'сезон';
const s2 = 'сезона';
const s3 = 'сезонов';

describe('pluralize', () => {
    it('should return expected result', () => {
        expect(pluralize(1, s1, s2, s3)).toEqual(s1);
        expect(pluralize(11, s1, s2, s3)).toEqual(s3);
        expect(pluralize(8, s1, s2, s3)).toEqual(s3);
        expect(pluralize(2, s1, s2, s3)).toEqual(s2);
        expect(pluralize(5, s1, s2, s3)).toEqual(s3);
        expect(pluralize(54, s1, s2, s3)).toEqual(s2);
        expect(pluralize(21, s1, s2, s3)).toEqual(s1);
        expect(pluralize(32, s1, s2, s3)).toEqual(s2);
    });
});

describe('pluralizeStr', () => {
    it('should return expected result', () => {
        expect(pluralizeStr(1, s1, s2, s3)).toMatchInlineSnapshot('"1 сезон"');
        expect(pluralizeStr(11, s1, s2, s3)).toMatchInlineSnapshot('"11 сезонов"');
        expect(pluralizeStr(4, s1, s2, s3)).toMatchInlineSnapshot('"4 сезона"');
    });
});
