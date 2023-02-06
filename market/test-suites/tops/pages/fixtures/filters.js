const paymentTypesFilter = {
    id: 'payments',
    type: 'enum',
    name: 'Способы оплаты',
    subType: '',
    kind: 2,
};

const paymentTypesFilterValues = [
    {
        initialFound: 1,
        found: 1,
        value: 'Картой на сайте',
        id: 'prepayment_card',
    },
    {
        initialFound: 1,
        found: 1,
        value: 'Картой курьеру',
        id: 'delivery_card',
    },
    {
        initialFound: 1,
        found: 1,
        value: 'Наличными курьеру',
        id: 'delivery_cash',
    },
];

const priceFilterValues = [{
    min: '100',
    max: '1000',
    initialMax: '250010',
    initialMin: '100',
    id: 'found',
}];

const priceFilterValuesWithCheckedValue = [{
    min: '100',
    max: '1000',
    initialMax: '250010',
    initialMin: '100',
    id: 'chosen',
    checked: true,
}];

const glPriceFilterPresetValues = [
    {
        initialFound: 3357,
        min: '100',
        max: '1000',
        unit: 'RUR',
        found: 3357,
        value: '100 – 1000',
        id: '100v1000',
    },
    {
        initialFound: 4348,
        max: '5000',
        unit: 'RUR',
        found: 4348,
        value: '2000 – 5000',
        min: '2000',
        id: '2000v5000',
    },
    {
        initialFound: 5639,
        max: '10000',
        unit: 'RUR',
        found: 5639,
        value: '5000 – 10000',
        min: '5000',
        id: '5000v10000',
    },
    {
        initialFound: 6284,
        found: 6284,
        value: '10000 – …',
        min: '10000',
        id: '10000v',
    },
];

const priceFilter = {
    id: 'glprice',
    type: 'number',
    name: 'Цена',
    subType: '',
    kind: 2,
    presetValues: glPriceFilterPresetValues,
};

const cpaTypesFilter = [
    {
        value: '1',
        found: 1,
        checked: true,
    },
    {
        value: '0',
        found: 1,
    },
];

export {
    paymentTypesFilter,
    paymentTypesFilterValues,

    priceFilter,
    priceFilterValues,
    priceFilterValuesWithCheckedValue,

    cpaTypesFilter,
};

