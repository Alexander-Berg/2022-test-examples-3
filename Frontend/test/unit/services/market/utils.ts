/* eslint-disable */
import test from 'ava';
import { groupMarketModelsByCategory } from '../../../../services/market/utils';
import { SerializedMarketModel } from '../../../../services/market/types';
import { sortByName } from '../../../_helpers';

test('groupMarketModelsByCategory: groups empty list', t => {
    t.deepEqual(groupMarketModelsByCategory([]), []);
});

test('groupMarketModelsByCategory: groups one device', t => {
    const marketModel: SerializedMarketModel = {
        id: '1',
        categoryId: 1,
        categoryName: '1',
        name: '1',
        picture: '1',
        vendorId: 1,
        vendorName: '1',
    };

    const categories = groupMarketModelsByCategory([marketModel]);

    t.deepEqual(categories, [
        {
            brandIds: [1],
            devicesCount: 1,
            id: 1,
            name: '1',
        },
    ]);
});

test('groupMarketModelsByCategory: groups two devices with different categories', t => {
    const marketModel1: SerializedMarketModel = {
        id: '1',
        categoryId: 1,
        categoryName: '1',
        name: '1',
        picture: '1',
        vendorId: 1,
        vendorName: '1',
    };

    const marketModel2: SerializedMarketModel = {
        id: '2',
        categoryId: 2,
        categoryName: '2',
        name: '2',
        picture: '2',
        vendorId: 2,
        vendorName: '2',
    };

    const categories = groupMarketModelsByCategory([marketModel1, marketModel2]);

    t.deepEqual(sortByName(categories), [
        {
            brandIds: [1],
            devicesCount: 1,
            id: 1,
            name: '1',
        },
        {
            brandIds: [2],
            devicesCount: 1,
            id: 2,
            name: '2',
        },
    ]);
});

test('groupMarketModelsByCategory: groups two devices with same category', t => {
    const marketModel1: SerializedMarketModel = {
        id: '1',
        categoryId: 1,
        categoryName: '1',
        name: '1',
        picture: '1',
        vendorId: 1,
        vendorName: '1',
    };

    const marketModel2: SerializedMarketModel = {
        id: '2',
        categoryId: 1,
        categoryName: '1',
        name: '2',
        picture: '2',
        vendorId: 2,
        vendorName: '2',
    };

    const categories = groupMarketModelsByCategory([marketModel1, marketModel2]);

    t.deepEqual(sortByName(categories), [
        {
            brandIds: [1, 2],
            devicesCount: 2,
            id: 1,
            name: '1',
        },
    ]);
});

test('groupMarketModelsByCategory: groups many devices with many brands and categories', t => {
    const marketModel1: SerializedMarketModel = {
        id: '1',
        categoryId: 1,
        categoryName: '1',
        name: '1',
        picture: '1',
        vendorId: 1,
        vendorName: '1',
    };

    const marketModel2: SerializedMarketModel = {
        id: '2',
        categoryId: 1,
        categoryName: '1',
        name: '2',
        picture: '2',
        vendorId: 2,
        vendorName: '2',
    };

    const marketModel3: SerializedMarketModel = {
        id: '3',
        categoryId: 2,
        categoryName: '2',
        name: '2',
        picture: '2',
        vendorId: 2,
        vendorName: '2',
    };

    const marketModel4: SerializedMarketModel = {
        id: '4',
        categoryId: 2,
        categoryName: '2',
        name: '2',
        picture: '2',
        vendorId: 3,
        vendorName: '3',
    };

    const categories = groupMarketModelsByCategory([marketModel1, marketModel2, marketModel3, marketModel4]);

    t.deepEqual(sortByName(categories), [
        {
            brandIds: [1, 2],
            devicesCount: 2,
            id: 1,
            name: '1',
        },
        {
            brandIds: [2, 3],
            devicesCount: 2,
            id: 2,
            name: '2',
        },
    ]);
});
