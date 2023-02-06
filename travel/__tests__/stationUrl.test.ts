import {
    FILTER_ALL,
    FILTER_EMPTY,
    FILTER_TODAY,
    SPAN_DAY,
    SPAN_SCHEDULE,
    SPAN_TOMORROW,
} from '../../station/stationConstants';

import StationEventList from '../../../interfaces/state/station/StationEventList';
import {TransportType} from '../../transportType';
import StationType from '../../../interfaces/state/station/StationType';
import Lang from '../../../interfaces/Lang';
import Tld from '../../../interfaces/Tld';
import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';
import StationSubtype from '../../../interfaces/state/station/StationSubtype';
import DateRobot from '../../../interfaces/date/DateRobot';
import StationTime from '../../../interfaces/state/station/StationTime';

import {getDateForUrl, stationsListUrl, stationUrl} from '../stationUrl';

const stationId = 123456;
const stationType = StationType.railroad;
const invalidDate = '0000-14-02 12:00:00';
const tld = Tld.ru;
const language = Lang.ru;

describe('stationUrl', () => {
    it('Если передан параметр event, должен корректно его обработать', () => {
        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                event: StationEventList.arrival,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/?event=${StationEventList.arrival}`);

        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                event: StationEventList.departure,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/`);
    });

    it('Должен вернуть ссылку с параметрами в правильном порядке', () => {
        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                direction: 'msktvr',
                date: '2019-06-05' as DateRobot,
                event: StationEventList.arrival,
                time: StationTime['p0-2'],
                search: 'test',
                tld,
                language,
            }),
        ).toBe(
            `/station/${stationId}/?event=${StationEventList.arrival}&date=2019-06-05&direction=msktvr&time=00%3A00-02%3A00&search=test`,
        );
    });

    it('Должен вернуть ссылку с корректным типом транспорта для нового роутинга', () => {
        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                subtype: StationSubtype.train,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/${TransportType.train}/`);

        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                subtype: StationSubtype.train,
                mainSubtype: StationSubtype.train,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/`);

        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.train,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/${TransportType.suburban}/`);
    });

    it('В случае, если передан миссив подтипов для станции, и указанного subtype нет в этом массиве, не нужно добавлять subtype в ссылку', () => {
        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                subtypes: [StationSubtype.suburban, StationSubtype.train],
                subtype: StationSubtype.suburban,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/${TransportType.suburban}/`);

        expect(
            stationUrl({
                id: stationId,
                type: stationType,
                subtypes: [StationSubtype.train],
                subtype: StationSubtype.suburban,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/`);
    });

    it('Не будет добавлять type в ссылку, если он единственно возможный для данного типа станции', () => {
        expect(
            stationUrl({
                id: stationId,
                type: StationType.bus,
                subtype: StationSubtype.schedule,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/`);

        expect(
            stationUrl({
                id: stationId,
                type: StationType.plane,
                subtype: StationSubtype.plane,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/`);

        expect(
            stationUrl({
                id: stationId,
                type: StationType.railroad,
                subtype: StationSubtype.suburban,
                tld,
                language,
            }),
        ).toBe(`/station/${stationId}/${StationSubtype.suburban}/`);
    });
});

describe('getDateForUrl', () => {
    it('Если передан пустой объект, возвращаем пустую строку', () => {
        expect(getDateForUrl({})).toBe('');
    });

    it('Если передан параметр span, должен корректно транслировать его', () => {
        expect(
            getDateForUrl({
                span: SPAN_TOMORROW,
            }),
        ).toBe(DateSpecialValue.tomorrow);

        expect(
            getDateForUrl({
                span: SPAN_DAY,
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                span: SPAN_SCHEDULE,
            }),
        ).toBe(DateSpecialValue.allDays);

        expect(
            getDateForUrl({
                span: 'invalid_value',
            }),
        ).toBe('');
    });

    it('Если в запросе передан параметр filter, должен корректно транслировать его', () => {
        expect(
            getDateForUrl({
                filter: FILTER_TODAY,
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                filter: FILTER_EMPTY,
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                filter: FILTER_ALL,
            }),
        ).toBe(DateSpecialValue.allDays);

        expect(
            getDateForUrl({
                filter: undefined,
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                filter: 'invalid_value',
            }),
        ).toBe('');

        // Проверяем что filter имеет приоритет над span
        expect(
            getDateForUrl({
                filter: FILTER_TODAY,
                span: SPAN_TOMORROW,
            }),
        ).toBe('');
    });

    it('Если в запросе передан параметр start, должен корректно транслировать его', () => {
        expect(
            getDateForUrl({
                start: '2019-06-05 13:05:00',
            }),
        ).toBe('2019-06-05');

        expect(
            getDateForUrl({
                start: invalidDate,
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                start: '',
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                start: undefined,
            }),
        ).toBe('');

        // Проверяем что start имеет приоритет над span
        expect(
            getDateForUrl({
                start: '2019-06-05 13:05:00',
                span: SPAN_TOMORROW,
            }),
        ).toBe('2019-06-05');

        // Проверяем что start имеет приоритет над filter
        expect(
            getDateForUrl({
                start: '2019-06-05 13:05:00',
                filter: FILTER_TODAY,
            }),
        ).toBe('2019-06-05');
    });

    it('Если в запросе передан параметр date и он валидный, должен использовать именно его', () => {
        expect(
            getDateForUrl({
                date: '2019-06-05' as DateRobot,
            }),
        ).toBe('2019-06-05');

        expect(
            getDateForUrl({
                date: '2019-06-05' as DateRobot,
                span: SPAN_TOMORROW,
            }),
        ).toBe('2019-06-05');

        expect(
            getDateForUrl({
                date: '2019-06-05' as DateRobot,
                filter: FILTER_TODAY,
            }),
        ).toBe('2019-06-05');

        expect(
            getDateForUrl({
                date: '2019-06-05' as DateRobot,
                start: '2019-05-19',
            }),
        ).toBe('2019-06-05');
    });

    it('Если передан невалидный date, то не должен использовать его', () => {
        expect(
            getDateForUrl({
                date: invalidDate as DateRobot,
            }),
        ).toBe('');

        expect(
            getDateForUrl({
                date: invalidDate as DateRobot,
                start: '2019-06-06',
            }),
        ).toBe('2019-06-06');
    });
});

describe('stationsListUrl', () => {
    it('Вернет URL списка станций', () => {
        const type = TransportType.train;
        const cityId = 1;

        expect(stationsListUrl(type, cityId, tld, language)).toBe(
            `/stations/${type}/?city=${cityId}`,
        );
    });
});
