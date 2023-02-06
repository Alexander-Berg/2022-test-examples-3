import { formatCarNumber } from 'entities/Car/helpers/formatCarNumber/formatCarNumber';

describe('formatCarNumber', function () {
    it('works with empty params', function () {
        expect(formatCarNumber('')).toMatchSnapshot();
    });

    it('works with full params', function () {
        ['АБВ', '123', 'АБВ 123 АБВ', '123 АБВ 123', '1 2 3 А Б В', 'В236ЕХ797', 'в236ех797', 'АМ05423'].forEach(
            (string) => {
                expect(formatCarNumber(string)).toMatchSnapshot();
            },
        );
    });
});
