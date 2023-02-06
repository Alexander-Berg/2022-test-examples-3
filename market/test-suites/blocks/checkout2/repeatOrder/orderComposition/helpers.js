// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as vitamins from '@self/root/src/spec/hermione/kadavr-mock/report/vitaminsLowCost';
import * as televizor from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import * as sock from '@self/root/src/spec/hermione/kadavr-mock/report/sock';
import * as largeCargoType from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as dropship from '@self/root/src/spec/hermione/kadavr-mock/report/dropship';
import * as dresser from '@self/root/src/spec/hermione/kadavr-mock/report/dresser';
import * as alcohol from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';

export const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export const bigCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }, {
            skuMock: vitamins.skuMock,
            offerMock: vitamins.offerMock,
            count: 1,
        }, {
            skuMock: televizor.skuMock,
            offerMock: televizor.offerMock,
            count: 1,
        }, {
            skuMock: sock.skuMock,
            offerMock: sock.offerMock,
            count: 1,
        }, {
            skuMock: largeCargoType.skuMock,
            offerMock: largeCargoType.offerMock,
            count: 1,
        }, {
            skuMock: farma.skuMock,
            offerMock: farma.offerMock,
            count: 1,
        }, {
            skuMock: dsbs.skuPhoneMock,
            offerMock: dsbs.offerPhoneMock,
            count: 1,
        }, {
            skuMock: dropship.skuPhoneMock,
            offerMock: dropship.offerPhoneMock,
            count: 1,
        }, {
            skuMock: dresser.skuMock,
            offerMock: dresser.offerMock,
            count: 1,
        }, {
            skuMock: alcohol.skuMock,
            offerMock: alcohol.offerMock,
            count: 1,
        }],
    }),
];
