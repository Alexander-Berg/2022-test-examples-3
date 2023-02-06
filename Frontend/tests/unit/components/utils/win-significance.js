const { getSignificanceLvl, getPValueLabel, getWinSignLabel } = require('../../../../src/client/components/utils/win-significance');

describe('components/utils/win-significance', () => {
    describe('getSignificanceLvl', () => {
        it('должен возвращать число, которое определяет насколько значима победа', () => {
            const level = getSignificanceLvl(0.5);

            assert.isNumber(level);
            assert.equal(level, 0);
        });

        it('должен возвращать null, если pValue не является числом или не задано', () => {
            assert.isNull(getSignificanceLvl('asd'));
            assert.isNull(getSignificanceLvl(null));
            assert.isNull(getSignificanceLvl());
        });
    });

    describe('getPValueLabel', () => {
        it('должен возвращать метку с числом, округленным до 4-й цифры после запятой, если передано число больше нуля', () => {
            assert.equal(getPValueLabel(0.12346), 'p = 0.1235');
        });

        it('должен возвращать метку с нулем без знаков после запятой, если передан нуль', () => {
            assert.equal(getPValueLabel(0), 'p = 0');
        });

        it('должен возвращать вопросительный знак, если передано не число', () => {
            assert.equal(getPValueLabel('0.12345'), 'p = ?');
            assert.equal(getPValueLabel(), 'p = ?');
            assert.equal(getPValueLabel(null), 'p = ?');
            assert.equal(getPValueLabel(undefined), 'p = ?');
        });
    });

    describe('getWinSignLabel', () => {
        it('должен возвращать метку с двумя вопросительными знаками, если передано не число', () => {
            assert.equal(getWinSignLabel('0'), '??');
            assert.equal(getWinSignLabel(), '??');
            assert.equal(getWinSignLabel(null), '??');
        });

        it('должен возвращать корректную метку, соответствующую уровню победы одной системы над другой', () => {
            assert.equal(getWinSignLabel(0, true), '≈');
            assert.equal(getWinSignLabel(0, false), '≈');
            assert.equal(getWinSignLabel(1, true), '⩾');
            assert.equal(getWinSignLabel(1, false), '⩽');
            assert.equal(getWinSignLabel(2, true), '>');
            assert.equal(getWinSignLabel(2, false), '<');
            assert.equal(getWinSignLabel(3, true), '≫');
            assert.equal(getWinSignLabel(3, false), '≪');
        });
    });
});
