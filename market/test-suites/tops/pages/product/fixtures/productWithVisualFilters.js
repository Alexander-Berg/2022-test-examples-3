import {times} from 'ambar';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {
    mergeState,
    createOffer,
    createPrice,
    createFilter,
    createFilterValue,
    createSku,
    createEntityFilter,
    createEntityFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

const COLOR_FILTER_ID = '14871214';
const BOOLEAN_FILTER_ID = '5085139';
const ENUM_FILTER_ID = '19172750';
const createFilterIds = count => times(x => x + 1, count);

const createRandomFilterColorValue = ({value, id, ...rest}) => {
    let result = {
        value,
        id,
        found: 9,
        initialFound: 9,
        picker: {
            groupId: '364668',
            entity: 'photo',
            imageName: 'model_option-picker-14209841-15277521--7949f9c422f927894c4b17ecb3ce59d6',
            namespace: 'get-mpic',
        },
        code: '#808080',
    };
    if (rest) {
        result = {
            ...result,
            ...rest,
        };
    }
    return result;
};

const colorFilterMock = {
    type: 'enum',
    subType: 'image_picker',
    name: 'Цвет товара',
    kind: 2,
    isGuruLight: true,
};

const createColorFilter = () => createFilter(colorFilterMock, COLOR_FILTER_ID);


const buildColorFilter = (filterValueIds, createFilterValueWithAddParamsCb) => {
    const colorFilter = createColorFilter();

    const filterValues = filterValueIds.map(id => {
        let additionalParams = {};
        if (createFilterValueWithAddParamsCb) {
            additionalParams = createFilterValueWithAddParamsCb(id);
        }
        const filterValue = createRandomFilterColorValue({value: id, id: String(id), ...additionalParams});
        return createFilterValue(filterValue, COLOR_FILTER_ID, filterValue.id);
    });

    return mergeState([
        colorFilter,
        ...filterValues,
    ]);
};

const UPDATED_SHOP_NAME = 'UpdatedTestShopName';

const testShop = {
    id: 123,
    qualityRating: 4,
    overallGradesCount: 111,
    name: 'test shop',
    slug: 'test-shop',
};

const createOfferMock = rest => (
    {
        shop: testShop,
        price: createPrice(1000),
        urls: {
            encrypted: '/redir/',
        },
        ...rest,
    }
);

const buildProductOffersResultsState = (offersCount = 2) => {
    const offers = [];

    for (let i = 0; i < offersCount; i++) {
        offers.push(
            createOffer(
                createOfferMock()
            )
        );
    }

    return mergeReportState([
        productWithPicture,
        ...offers,

        {
            data: {
                search: {
                    total: offersCount,
                    totalOffers: offersCount,
                    totalOffersBeforeFilters: offersCount,
                    totalModels: 0,
                },
            },
        },
    ]);
};

const buildBooleanFilter = filterValueIds => filterValueIds.map(id => createFilter({
    id,
    type: 'boolean',
    name: 'Wi-Fi',
    xslname: 'Home80211',
    subType: '',
    kind: 2,
    isGuruLight: true,
    hasBoolNo: true,
    position: 21,
    noffers: 39,
    values: [
        {
            initialFound: 39,
            found: 39,
            value: '1',
            priceMin: {
                currency: 'RUR',
                value: '41990',
            },
            id: '1',
        },
        {
            initialFound: 10,
            found: 10,
            value: '0',
            priceMin: {
                currency: 'RUR',
                value: '25000',
            },
            id: '0',
        },
    ],
}, BOOLEAN_FILTER_ID));


const enumFilterValuesMock = [{
    slug: '',
    found: 1,
    value: 'enumValue1',
},
{
    found: 1,
    slug: 'smartfon-apple-iphone-xr-64gb-krasnyi',
    value: 'enumValue2',
},
];

const enumFilterMock = {
    isGuruLight: true,
    kind: 2,
    meta: {},
    name: 'Enum filter',
    precision: 2,
    subType: '',
    type: 'enum',
    valuesGroups: [],
};

const SKU_TITLES_RAW = 'New Sku title';

const createOffersWithChechedFiltersState = ({applyColorFilterId, applyTypeFilterId}) => {
    const checkedColorFilter = createRandomFilterColorValue({id: String(applyColorFilterId), value: applyColorFilterId, checked: true});

    const enumFilterValues = enumFilterValuesMock.map((enumFilterValue, i) => {
        const id = String(i + 1);
        return createFilterValue({...enumFilterValue, marketSku: id, checked: enumFilterValue.value === applyTypeFilterId}, ENUM_FILTER_ID, id);
    });

    const filterValueIds = createFilterIds(15);

    const colorFilerValues = filterValueIds.map(id =>
        createFilterValue(
            createRandomFilterColorValue({id: String(id), value: id, checked: id === applyColorFilterId}),
            COLOR_FILTER_ID,
            String(id)
        )
    );
    const colorFilter = createColorFilter();
    const offerIds = times(x => x + 1, 4);
    const offersWithFilter = offerIds.map(offerId => mergeState([
        createOffer(createOfferMock({marketSku: applyColorFilterId, shop: {...testShop, name: UPDATED_SHOP_NAME}}), offerId),
        createEntityFilter(colorFilterMock, 'offer', offerId, COLOR_FILTER_ID),
        createEntityFilterValue(checkedColorFilter, offerId, COLOR_FILTER_ID, checkedColorFilter.id),
    ]));
    const sku = createSku({titles: {raw: SKU_TITLES_RAW}}, applyColorFilterId);
    const enumFilter = createFilter(enumFilterMock, String(ENUM_FILTER_ID));
    const state = mergeState([
        ...offersWithFilter,
        colorFilter,
        ...colorFilerValues,
        enumFilter,
        ...enumFilterValues,
        sku,
    ]);
    return state;
};


export {
    buildProductOffersResultsState,
    buildColorFilter,
    createFilterIds,
    COLOR_FILTER_ID,
    ENUM_FILTER_ID,
    buildBooleanFilter,
    enumFilterValuesMock,
    enumFilterMock,
    createOffersWithChechedFiltersState,
    SKU_TITLES_RAW,
    UPDATED_SHOP_NAME,
};
