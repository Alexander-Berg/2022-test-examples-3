jest.disableAutomock();

import joinStaticPriceAnswers from '../joinStaticPriceAnswers';

const tariff1 = {key: '1'};
const tariff2 = {key: '2'};
const tariff3 = {key: '3'};

const staticTariffs = {tariffs: [tariff1]};
const allDaysTariffs = {tariffs: [tariff2]};
const suburbanTariffs = {tariffs: [tariff3]};

describe('joinStaticPriceAnswers', () => {
    it('Вернет пустой массив тарифов, если ни один из тарифов не пришел', () => {
        expect(joinStaticPriceAnswers([])).toEqual([]);
    });

    it('Вернёт объединенный список тарифов', () => {
        expect(
            joinStaticPriceAnswers([
                {staticTariffs},
                {allDaysTariffs},
                {suburbanTariffs},
            ]),
        ).toEqual([tariff1, tariff2, tariff3]);
    });
});
