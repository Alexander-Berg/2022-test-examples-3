import {TransportType} from '../../transportType';

import {TrainPartner} from '../trainPartners';
import {OrderUrlOwner} from '../../segments/tariffClasses';

import shouldUseLinkToTrains from '../shouldUseLinkToTrains';

const segmentForAllDays = {
    transport: {
        code: TransportType.train,
    },
    trainPartners: [TrainPartner.im],
    runDays: {},
    thread: {
        firstCountryCode: 'RU',
        lastCountryCode: 'RU',
    },
    stationFrom: {
        country: {
            code: 'RU',
        },
    },
    stationTo: {
        country: {
            code: 'RU',
        },
    },
};

const segmentForDate = {
    transport: {
        code: TransportType.train,
    },
    tariffs: {},
};

const tariffFromTrains = {
    classes: {
        compartment: {
            trainOrderUrl: 'someUrl',
            trainOrderUrlOwner: OrderUrlOwner.trains,
        },
    },
};

const transferTariffFromTrains = {
    classes: {
        compartment: {
            orderUrl: 'someUrl',
            trainOrderUrlOwner: OrderUrlOwner.trains,
        },
    },
};

const tariffFromUFS = {
    classes: {
        compartment: {
            trainOrderUrl: 'someUrl',
            trainOrderUrlOwner: OrderUrlOwner.ufs,
        },
    },
};

describe('shouldUseLinkToTrains для поиска на все дни', () => {
    it('В случае, если сегмент - это не поезд и не электричка с тарифом поезда, то вернет false', () => {
        expect(
            shouldUseLinkToTrains({
                ...segmentForAllDays,
                transport: {code: TransportType.bus},
            }),
        ).toBe(false);
    });

    it('Вернёт true для поездов внутри бывшего СССР (за исключением Украины)', () => {
        expect(shouldUseLinkToTrains(segmentForAllDays)).toBe(true);
    });

    it('Вернёт false если продажа на своей стороне отключена', () => {
        expect(
            shouldUseLinkToTrains({
                ...segmentForAllDays,
                trainPartners: [],
            }),
        ).toBe(false);
    });

    it('Вернёт false для самолета', () => {
        expect(
            shouldUseLinkToTrains({
                ...segmentForAllDays,
                transport: {
                    code: 'plane',
                },
            }),
        ).toBe(false);
    });

    it('Вернёт false для украинского сегмента', () => {
        expect(
            shouldUseLinkToTrains({
                ...segmentForAllDays,
                stationFrom: {
                    country: {
                        code: 'UA',
                    },
                },
            }),
        ).toBe(false);
    });

    it('Вернёт false для международного поезда', () => {
        expect(
            shouldUseLinkToTrains({
                ...segmentForAllDays,
                thread: {
                    firstCountryCode: 'RU',
                    lastCountryCode: 'FR',
                },
            }),
        ).toBe(false);
    });

    it('Вернёт false если в админке выставлено соответствующее поле oldUfsOrder', () => {
        expect(
            shouldUseLinkToTrains({
                ...segmentForAllDays,
                oldUfsOrder: true,
            }),
        ).toBe(false);
    });
});

describe('shouldUseLinkToTrains для поиска на конкретную дату', () => {
    it(
        'В случае, если сегмент - это не поезд и не электричка с тарифом поезда,' +
            ' то вернет false',
        () => {
            expect(
                shouldUseLinkToTrains({
                    ...segmentForDate,
                    tariffs: tariffFromTrains,
                    transport: {code: TransportType.bus},
                }),
            ).toBe(false);
        },
    );

    it(
        'В случае, если сегмент - электричка с тарифом поезда со ссылками,' +
            ' ведущими на поезда то вернет true',
        () => {
            const segment = {
                ...segmentForDate,
                tariffs: tariffFromTrains,
                transport: {code: TransportType.suburban},
                hasTrainTariffs: true,
            };

            expect(shouldUseLinkToTrains(segment)).toBe(true);

            expect(
                shouldUseLinkToTrains({
                    ...segment,
                    tariffs: transferTariffFromTrains,
                }),
            ).toBe(true);
        },
    );

    it(
        'В случае, если сегмент - поезд со ссылками,' +
            ' ведущими на поезда то вернет true',
        () => {
            const segment = {
                ...segmentForDate,
                tariffs: tariffFromTrains,
            };

            expect(shouldUseLinkToTrains(segment)).toBe(true);

            expect(
                shouldUseLinkToTrains({
                    ...segment,
                    tariffs: transferTariffFromTrains,
                }),
            ).toBe(true);
        },
    );

    it(
        'В случае, если сегмент - поезд со ссылками,' +
            ' ведущими на УФС то вернет false',
        () => {
            expect(
                shouldUseLinkToTrains({
                    ...segmentForDate,
                    tariffs: tariffFromUFS,
                }),
            ).toBe(false);
        },
    );
});
