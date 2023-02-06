import ISegment from '../../../../../interfaces/segment/ISegment';
import ISearchMeta from '../../../../../interfaces/state/search/ISearchMeta';
import ISegmentTariffs from '../../../../../interfaces/segment/ISegmentTariffs';
import ISegmentTariffClassFromBackend from '../../../../../interfaces/segment/ISegmentTariffClassFromBackend';
import ISegmentTariffClass from '../../../../../interfaces/segment/ISegmentTariffClass';
import {TransportType} from '../../../../transportType';
import Tld from '../../../../../interfaces/Tld';
import {OrderUrlOwner} from '../../../tariffClasses';
import CurrencyCode from '../../../../../interfaces/CurrencyCode';

export const meta = {
    searchForm: {},
    flags: {},
    context: {
        from: {
            key: 'c54',
        },
        to: {
            key: 'c50',
        },
    },
    currencies: {
        nationalCurrency: CurrencyCode.rub,
        preferredCurrency: CurrencyCode.usd,
    },
    isProduction: true,
    tld: Tld.ru,
} as ISearchMeta;

export const busTariffs = {
    classes: {
        bus: {
            price: {
                value: 10,
                currency: CurrencyCode.usd,
            },
            orderUrl: '//buy/',
        },
    },
} as ISegmentTariffs<ISegmentTariffClassFromBackend>;

export const updatedBusTariffs = {
    classes: {
        bus: {
            nationalPrice: {
                value: 650,
                currency: CurrencyCode.rub,
            },
            price: {
                value: 10,
                currency: CurrencyCode.usd,
            },
            orderUrl: '//buy/',
        },
    },
} as ISegmentTariffs<ISegmentTariffClass>;

export const busSegment = {
    transport: {
        code: TransportType.bus,
    },
    tariffs: busTariffs,
} as unknown as ISegment;

const priceUSD = {
    price: {
        value: 10,
        currency: CurrencyCode.usd,
    },
};

const nationalPrice = {
    nationalPrice: {
        value: 650,
        currency: CurrencyCode.rub,
    },
};

const orderUrl = 'someOrderUrl';
const trainOrderUrl = 'someTrainOrderUrl';
const UFSOrderUrl = 'someUFSLink';
const unknownOrderUrl = 'unknownLink';

export const trainTariffsFromTrains = {
    classes: {
        suite: {
            ...priceUSD,
            orderUrl,
            trainOrderUrl,
            trainOrderUrlOwner: OrderUrlOwner.trains,
        },
    },
} as ISegmentTariffs<ISegmentTariffClassFromBackend>;

export const trainTariffsFromUfs = {
    classes: {
        suite: {
            ...priceUSD,
            orderUrl,
            trainOrderUrl: UFSOrderUrl,
            trainOrderUrlOwner: OrderUrlOwner.ufs,
        },
    },
} as ISegmentTariffs<ISegmentTariffClassFromBackend>;

export const trainTariffsFromUnknown = {
    classes: {
        suite: {
            ...priceUSD,
            orderUrl,
            trainOrderUrl: unknownOrderUrl,
            trainOrderUrlOwner: null,
        },
    },
} as ISegmentTariffs<ISegmentTariffClassFromBackend>;

export const trainTariffsWithUfsLink = {
    classes: {
        suite: {
            ...priceUSD,
            ...nationalPrice,
            trainOrderUrl: UFSOrderUrl,
            trainOrderUrlOwner: OrderUrlOwner.ufs,
            orderUrl: UFSOrderUrl,
        },
    },
} as ISegmentTariffs<ISegmentTariffClass>;

export const trainTariffsWithTrainsLink = {
    classes: {
        suite: {
            ...priceUSD,
            ...nationalPrice,
            trainOrderUrl,
            trainOrderUrlOwner: OrderUrlOwner.trains,
            orderUrl: 'absoluteUrlToTrains',
        },
    },
} as ISegmentTariffs<ISegmentTariffClass>;

export const trainTariffsWithUnknownLink = {
    classes: {
        suite: {
            ...priceUSD,
            ...nationalPrice,
            trainOrderUrl: unknownOrderUrl,
            trainOrderUrlOwner: null,
            orderUrl: '',
        },
    },
} as ISegmentTariffs<ISegmentTariffClass>;

export const trainSegment = {
    number: 2,
    transport: {
        code: TransportType.train,
    },
    stationTo: {
        codes: {express: 2000200},
    },
    stationFrom: {
        codes: {express: 8000800},
    },
    tariffs: [],
} as unknown as ISegment;
