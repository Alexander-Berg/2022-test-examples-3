import StationEventList from '../../../interfaces/state/station/StationEventList';
import StationSubtype from '../../../interfaces/state/station/StationSubtype';
import StationType from '../../../interfaces/state/station/StationType';

import getTitleForPage from '../getTitleForPage';

const title = 'Шарташ';
const settlementTitle = 'Екатеринбург';
const fullTitle = 'станция Шарташ';
const fullTitleDative = 'станции Шарташ';
const type = StationType.railroad;

describe('getTitleForPage', () => {
    it('Если не задан fullTitle вернёт результат с title, а если задан - fullTitle (популярные заголовки)', () => {
        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
                settlementTitle,
            }),
        ).toBe('Расписание электричек: Шарташ (Екатеринбург)');

        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
                settlementTitle,
                fullTitle,
            }),
        ).toBe('Расписание электричек: станция Шарташ (Екатеринбург)');
    });

    it('Если не задан fullTitleDative вернёт результат с fullTitle или title, а если задан - fullTitleDative (не-популярные заголовки)', () => {
        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: false,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
                settlementTitle,
            }),
        ).toBe('Расписание электричек по Шарташ');

        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: false,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
                settlementTitle,
                fullTitle,
            }),
        ).toBe('Расписание электричек по станция Шарташ');

        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: false,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
                settlementTitle,
                fullTitle,
                fullTitleDative,
            }),
        ).toBe('Расписание электричек по станции Шарташ');
    });

    it('Вернёт соответствующий результат для event = departure и arrival', () => {
        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.train,
                settlementTitle,
            }),
        ).toBe('Расписание поездов: Шарташ (Екатеринбург). Прибытие');

        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.departure,
                currentSubtype: StationSubtype.train,
                settlementTitle,
            }),
        ).toBe('Расписание поездов: Шарташ (Екатеринбург). Отправление');
    });

    it('Вернёт соответствующий результат для subtype = train и suburban', () => {
        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
            }),
        ).toBe('Расписание электричек: Шарташ');

        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.train,
            }),
        ).toBe('Расписание поездов: Шарташ. Прибытие');
    });

    it('Если не задан settlement то вернёт без города, иначе с ним', () => {
        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
            }),
        ).toBe('Расписание электричек: Шарташ');
        expect(
            getTitleForPage({
                type,
                title,
                hasPopularTitle: true,
                event: StationEventList.arrival,
                currentSubtype: StationSubtype.suburban,
                settlementTitle,
            }),
        ).toBe('Расписание электричек: Шарташ (Екатеринбург)');
    });

    it('Для автобусной станции', () => {
        expect(
            getTitleForPage({
                type: StationType.bus,
                title,
                event: StationEventList.departure,
                hasPopularTitle: true,
                fullTitle,
            }),
        ).toBe(`Расписание автобусов: ${fullTitle}`);

        expect(
            getTitleForPage({
                type: StationType.bus,
                title,
                event: StationEventList.departure,
                hasPopularTitle: true,
                fullTitle,
                settlementTitle,
            }),
        ).toBe(`Расписание автобусов: ${fullTitle} (${settlementTitle})`);

        expect(
            getTitleForPage({
                type: StationType.bus,
                title,
                event: StationEventList.departure,
                hasPopularTitle: true,
                fullTitle: 'Автовокзал Екатеринбурга',
                settlementTitle: 'Екатеринбург',
            }),
        ).toBe('Расписание автобусов: Автовокзал Екатеринбурга');
    });

    it('Для станции водного транспорта', () => {
        expect(
            getTitleForPage({
                type: StationType.water,
                title,
                event: StationEventList.departure,
                hasPopularTitle: true,
                fullTitle,
            }),
        ).toBe(`${fullTitle}: расписание теплоходов`);

        expect(
            getTitleForPage({
                type: StationType.water,
                title,
                event: StationEventList.departure,
                hasPopularTitle: true,
                fullTitle,
                settlementTitle,
            }),
        ).toBe(`${fullTitle} (${settlementTitle}): расписание теплоходов`);

        expect(
            getTitleForPage({
                type: StationType.water,
                title,
                event: StationEventList.departure,
                hasPopularTitle: true,
                fullTitle: 'Исетский причал Екатеринбурга',
                settlementTitle: 'Екатеринбург',
            }),
        ).toBe('Исетский причал Екатеринбурга: расписание теплоходов');
    });
});
