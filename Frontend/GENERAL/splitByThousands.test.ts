import { splitBaseByThousands } from './splitByThousands';

describe('splitByThousands', () => {
    it('don\'t split when length < 4', () => {
        const actual = splitBaseByThousands('10');
        expect(actual).toEqual(['10']);
    });

    it('split by thousands', () => {
        const actual = splitBaseByThousands('1234567');
        expect(actual).toEqual(['1', '234', '567']);
    });
});
