import titleHasSettlementTitle from '../titleHasSettlementTitle';

describe('titleHasSettlementTitle', () => {
    it('Вернет true', () => {
        expect(
            titleHasSettlementTitle(
                'Воронеж, автостанция Юго-Западная',
                'воронеж',
            ),
        ).toBe(true);
        expect(
            titleHasSettlementTitle('Вокзал Екатеринбурга', 'Екатеринбург'),
        ).toBe(true);
    });

    it('Вернет false', () => {
        expect(
            titleHasSettlementTitle('автостанция Юго-Западная', 'воронеж'),
        ).toBe(false);
    });
});
