// Связана с экспериментом RASPFRONT-6951
import {from25to50, from50to75} from '../addBlablacarUtmFromDistance';

// eslint-disable-next-line no-duplicate-imports
import {addBlablacarUtmFromDistance} from '../addBlablacarUtmFromDistance';

const orderUrl =
    'https://someurl.ru/?comuto_cmkt=RU_YANDEXRASP_PSGR_TIMETABLE_none&utm_campaign=test';
const orderUrl25to50 = `https://someurl.ru/?comuto_cmkt=RU_YANDEXRASP_PSGR_TIMETABLE_none_${from25to50}&utm_campaign=test`;
const orderUrl50to75 = `https://someurl.ru/?comuto_cmkt=RU_YANDEXRASP_PSGR_TIMETABLE_none_${from50to75}&utm_campaign=test`;

describe('addBlablacarUtmFromDistance', () => {
    it(
        'Вернёт оригинальный урл, если distance не определён' +
            ' или если orderUrl - пустая строка или если мы на десктопе',
        () => {
            expect(
                addBlablacarUtmFromDistance({
                    orderUrl,
                }),
            ).toBe(orderUrl);

            expect(
                addBlablacarUtmFromDistance({
                    orderUrl: '',
                    distance: 50,
                }),
            ).toBe('');
        },
    );

    it('Для расстояния меньше 25 и больше 75 км вернёт оригинальный урл', () => {
        expect(
            addBlablacarUtmFromDistance({
                orderUrl,
                distance: 10,
            }),
        ).toBe(orderUrl);

        expect(
            addBlablacarUtmFromDistance({
                orderUrl,
                distance: 100,
            }),
        ).toBe(orderUrl);
    });

    it('Для расстояния между 25 и 50 км вернёт урл с нужной меткой', () => {
        expect(
            addBlablacarUtmFromDistance({
                orderUrl,
                distance: 30,
            }),
        ).toBe(orderUrl25to50);
    });

    it('Для расстояния между 50 и 75 км вернёт урл с нужной меткой', () => {
        expect(
            addBlablacarUtmFromDistance({
                orderUrl,
                distance: 60,
            }),
        ).toBe(orderUrl50to75);
    });
});
