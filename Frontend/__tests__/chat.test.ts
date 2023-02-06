import { isEtherChat } from '../chat';

describe('#isEtherChat', () => {
    it('должен вернуть true для эфирного чата', () => {
        expect(isEtherChat('0/7/41982ecac610fe988f266cfde5ad5a08')).toBeTruthy();
    });

    it('должен вернуть false для не эфирного чата', () => {
        expect(isEtherChat('0/0/9644f6ca-6fbe-42cf-97fb-dd6856b3ea04')).toBeFalsy();
    });

    it('должен вернуть false без переданного параметра', () => {
        expect(isEtherChat()).toBeFalsy();
    });
});
