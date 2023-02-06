import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import DeliveryWithCashOnDelivery from './delivery/cashOnDelivery';
import ClickAndCollectPlusLargeCargoType from './delivery/clickAndCollectPlusLargeCargoType';
import outletPointDeliveryDateExisting from './delivery/outletPointDeliveryDateExisting';
import saveDataAtMultiOrder from './hsch/saveDataAtMultiOrder';
import express from './express';

export default makeSuite('Первая покупка', {
    feature: 'Первая покупка',
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(DeliveryWithCashOnDelivery),
        prepareSuite(ClickAndCollectPlusLargeCargoType),
        prepareSuite(outletPointDeliveryDateExisting),
        prepareSuite(saveDataAtMultiOrder, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(express, {
            params: {
                isAuthWithPlugin: true,
            },
        })
    ),
});
