'use strict';

const {
    mergeState,
    createProduct,
    createFilter,
    createEntityFilter,
    createFilterValue,
    createEntityFilterValue,
} = require('../../../../mocks/Report/helpers');

const FILTER_IDS = {
    ANDROID: '13476053',
    NFC: '7013269',
    CPU_FREQ: '4925734',
    VENDOR: '7893318',
    ONSTOCK: 'onstock',
    GL_PRICE: 'glprice',
    MANUFACTURER_WARRANTY_PARAM: 'manufacturer_warranty',
    CREDIT_TYPE: 'credit-type',
    CPA: 'cpa',
};

const VENDOR_IDS = {
    ASUS: '152863',
    HTC: '955287',
};

const CREDIT_TYPES = {
    CREDIT: 'credit',
    INSTALLMENT: 'installment',
};

const ANDROID_FILTER_OPTIONS = {name: 'Android', type: 'boolean'};
const NFC_FILTER_OPTIONS = {name: 'NFC', type: 'boolean'};
const CPU_FREQ_FILTER_OPTIONS = {name: 'Частота процессора', type: 'number'};
const VENDOR_FILTER_OPTIONS = {name: 'Производитель', type: 'enum'};
const CREDIT_TYPE_FILTER_OPTIONS = {name: 'Покупка в кредит', type: 'radio'};

const androidFilter = createFilter(ANDROID_FILTER_OPTIONS, FILTER_IDS.ANDROID);
const androidHasBoolNoModifier = createFilter({hasBoolNo: true}, FILTER_IDS.ANDROID);
const androidFilterChecked = createFilterValue({found: 1, value: '1'}, FILTER_IDS.ANDROID, '1');
const androidFilterUnChecked = createFilterValue({found: 2, value: '0'}, FILTER_IDS.ANDROID, '0');

const mergedAndroidFilter = mergeState([
    androidFilter,
    androidFilterChecked,
    androidFilterUnChecked,
]);

const androidFilterHasBoolNo = mergeState([
    mergedAndroidFilter,
    androidHasBoolNoModifier,
]);

const nfcFilter = createFilter(ANDROID_FILTER_OPTIONS, FILTER_IDS.NFC);
const nfcFilterChecked = createFilterValue({found: 2, value: '1'}, FILTER_IDS.NFC, '1');
const nfcFilterUnChecked = createFilterValue({found: 3, value: '0'}, FILTER_IDS.NFC, '0');

const mergedNFCFilter = mergeState([
    nfcFilter,
    nfcFilterChecked,
    nfcFilterUnChecked,
]);

const cpuFreqFilter = createFilter(CPU_FREQ_FILTER_OPTIONS, FILTER_IDS.CPU_FREQ);
const cpuFreqFilterFoundValue = createFilterValue({}, FILTER_IDS.CPU_FREQ, 'found');

const mergedCpuFreqFilter = mergeState([
    cpuFreqFilter,
    cpuFreqFilterFoundValue,
]);

const vendorFilter = createFilter(VENDOR_FILTER_OPTIONS, FILTER_IDS.VENDOR);
const vendorAsus = createFilterValue({}, FILTER_IDS.VENDOR, VENDOR_IDS.ASUS);
const vendorHTC = createFilterValue({}, FILTER_IDS.VENDOR, VENDOR_IDS.HTC);

const mergedVendorFilter = mergeState([
    vendorFilter,
    vendorAsus,
    vendorHTC,
]);

const createPhone = productId => {
    const phone = createProduct({}, productId);
    const androidEntityFilter = createEntityFilter(
        ANDROID_FILTER_OPTIONS,
        'product',
        productId,
        FILTER_IDS.ANDROID,
    );

    return mergeState([
        phone,
        androidEntityFilter,
    ]);
};

const createAndroidPhone = productId => {
    const phone = createPhone(productId);
    const androidEntityFilterValue = createEntityFilterValue(
        {found: 1, value: '1'},
        productId,
        FILTER_IDS.ANDROID,
        '1',
    );

    return mergeState([
        phone,
        androidEntityFilterValue,
    ]);
};

const createiOSPhone = productId => {
    const phone = createPhone(productId);
    const androidEntityFilterValue = createEntityFilterValue(
        {found: 1, value: '0'},
        productId,
        FILTER_IDS.ANDROID,
        '0',
    );

    return mergeState([
        phone,
        androidEntityFilterValue,
    ]);
};

const createNFCPhone = productId => {
    const phone = createPhone(productId);
    const nfcEntityFilter = createEntityFilter(
        NFC_FILTER_OPTIONS,
        'product',
        productId,
        FILTER_IDS.NFC,
    );
    const nfcEntityFilterValue = createEntityFilterValue(
        {found: 1, value: '1'},
        productId,
        FILTER_IDS.NFC,
        '1',
    );

    return mergeState([
        phone,
        nfcEntityFilter,
        nfcEntityFilterValue,
    ]);
};

const createPhoneWithCpuFreq = (productId, freq) => {
    const phone = createProduct({}, productId);
    const cpuFreqEntityFilter = createEntityFilter(
        CPU_FREQ_FILTER_OPTIONS,
        'product',
        productId,
        FILTER_IDS.CPU_FREQ,
    );
    const cpuFreqFilterValue = createEntityFilterValue(
        {min: freq, max: freq},
        productId,
        FILTER_IDS.CPU_FREQ,
        'found',
    );

    return mergeState([
        phone,
        cpuFreqEntityFilter,
        cpuFreqFilterValue,
    ]);
};

const createPhoneWithVendor = (productId, vendorId) => {
    const phone = createProduct({}, productId);
    const vendorEntityFilter = createEntityFilter(
        VENDOR_FILTER_OPTIONS,
        'product',
        productId,
        FILTER_IDS.VENDOR,
    );
    const vendorFilterValue = createEntityFilterValue(
        {found: 1},
        productId,
        FILTER_IDS.VENDOR,
        vendorId,
    );

    return mergeState([
        phone,
        vendorEntityFilter,
        vendorFilterValue,
    ]);
};

const onstockFilter = createFilter({
    type: 'boolean',
    name: 'В продаже',
    subType: '',
    kind: 2,
}, 'onstock');
const disabledOnstockFilterValue = createFilterValue({
    value: '0',
    initialFound: 2,
    found: 2,
}, FILTER_IDS.ONSTOCK, '0');
const enabledOnstockFilterValue = createFilterValue({
    value: '1',
    initialFound: 2,
    found: 2,
}, FILTER_IDS.ONSTOCK, '1');

const mergedOnstockFilter = mergeState([
    onstockFilter,
    disabledOnstockFilterValue,
    enabledOnstockFilterValue,
]);

const checkedOnstockFilter = mergeState([
    mergedOnstockFilter,
    createFilterValue({checked: true}, FILTER_IDS.ONSTOCK, '1'),
]);


const cpaFilter = createFilter({
    type: 'boolean',
    name: 'Покупка на Маркете',
    subType: '',
    kind: 2,
}, 'onstock');

const disabledCpaFilterValue = createFilterValue({
    value: '0',
    initialFound: 2,
    found: 2,
}, FILTER_IDS.CPA, '0');

const enabledCpaFilterValue = createFilterValue({
    value: '1',
    initialFound: 2,
    found: 2,
}, FILTER_IDS.CPA, '1');

const mergedCpaFilter = mergeState([
    cpaFilter,
    disabledCpaFilterValue,
    enabledCpaFilterValue,
]);

const checkedCpaFilter = mergeState([
    mergedCpaFilter,
    createFilterValue({checked: true}, FILTER_IDS.CPA, '1'),
]);

const warrantyFilter = createFilter({
    type: 'boolean',
    name: 'Гарантия производитела',
    subType: '',
    kind: 2,
}, FILTER_IDS.MANUFACTURER_WARRANTY_PARAM);
const disabledWarrantyFilterValue = createFilterValue({
    value: '0',
    initialFound: 2,
    found: 2,
}, FILTER_IDS.MANUFACTURER_WARRANTY_PARAM, '0');
const enabledWarrantyFilterValue = createFilterValue({
    value: '1',
    initialFound: 2,
    found: 2,
}, FILTER_IDS.MANUFACTURER_WARRANTY_PARAM, '1');

const mergedWarrantyFilter = mergeState([
    warrantyFilter,
    disabledWarrantyFilterValue,
    enabledWarrantyFilterValue,
]);

const checkedWarrantyFilter = mergeState([
    mergedWarrantyFilter,
    createFilterValue({checked: true}, FILTER_IDS.MANUFACTURER_WARRANTY_PARAM, '1'),
]);

const creditTypeFilter = createFilter(CREDIT_TYPE_FILTER_OPTIONS, FILTER_IDS.CREDIT_TYPE);
const creditTypeCredit = createFilterValue({
    value: CREDIT_TYPES.CREDIT,
    found: 10,
    initialFound: 10,
}, FILTER_IDS.CREDIT_TYPE, CREDIT_TYPES.CREDIT);
const creditTypeInstallment = createFilterValue({
    value: CREDIT_TYPES.INSTALLMENT,
    found: 1,
    initialFound: 1,
}, FILTER_IDS.CREDIT_TYPE, CREDIT_TYPES.INSTALLMENT);

const mergedCreditTypeFilter = mergeState([
    creditTypeFilter,
    creditTypeCredit,
    creditTypeInstallment,
]);

const checkedCreditTypeFilter = mergeState([
    mergedCreditTypeFilter,
    createFilterValue({checked: true}, FILTER_IDS.CREDIT_TYPE, CREDIT_TYPES.CREDIT),
]);

const glPriceFilter = createFilter({
    type: 'number',
    name: 'Цена',
    subType: '',
    kind: 2,
}, FILTER_IDS.GL_PRICE);
const glPriceFoundValue = createFilterValue({
    max: '999',
    min: '0',
    initialMax: '999',
    initialMin: '0',
}, FILTER_IDS.GL_PRICE, 'found');

const mergedGlPriceFilter = mergeState([
    glPriceFilter,
    glPriceFoundValue,
]);

module.exports = {
    androidFilter: mergedAndroidFilter,
    nfcFilter: mergedNFCFilter,
    cpuFreqFilter: mergedCpuFreqFilter,
    vendorFilter: mergedVendorFilter,
    uncheckedOnstockFilter: mergedOnstockFilter,
    uncheckedWarrantyFilter: mergedWarrantyFilter,
    glPriceFilter: mergedGlPriceFilter,
    cpaFilter: mergedCpaFilter,
    checkedCpaFilter,
    checkedOnstockFilter,
    checkedWarrantyFilter,
    androidFilterHasBoolNo,
    createAndroidPhone,
    createiOSPhone,
    createNFCPhone,
    createPhoneWithCpuFreq,
    createPhoneWithVendor,
    uncheckedCreditTypeFilter: mergedCreditTypeFilter,
    checkedCreditTypeFilter,
    FILTER_IDS,
    VENDOR_IDS,
};
