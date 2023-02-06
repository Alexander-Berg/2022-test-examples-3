/* eslint-disable */
import test from 'ava';
import { serializeMarketModel } from '../../../services/market/utils';

const rawModel = {
    id: 123,
    name: 'test',
    kind: '',
    type: 'MODEL',
    isNew: false,
    description: 'test',
    photo: {
        width: 701,
        height: 663,
        url: 'https://test.com',
    },
    category: {
        id: 123,
        name: 'test-category',
        fullName: 'test-category',
        link: 'test',
    },
    vendor: {
        id: 1234,
        name: 'test-vendor',
        picture: 'test',
        link: 'test',
        site: 'test',
    },
    link: 'test',
    modelSpecificationsLink: 'test',
};

test('It should serialize correctly', t => {
    const serialized = serializeMarketModel(rawModel);

    t.deepEqual(serialized, {
        id: '123',
        categoryId: 123,
        vendorId: 1234,
        vendorName: 'test-vendor',
        categoryName: 'test-category',
        name: 'test',
        picture: 'https://test.com',
    });
});

test('It should serialize market model without category correctly', t => {
    const model = {
        ...rawModel,
        category: undefined,
    };
    const serialized = serializeMarketModel(model);

    t.deepEqual(serialized, {
        id: '123',
        categoryId: -1,
        vendorId: 1234,
        vendorName: 'test-vendor',
        categoryName: '',
        name: 'test',
        picture: 'https://test.com',
    });
});

test('It should serialize market model without vendor correctly', t => {
    const model = {
        ...rawModel,
        vendor: undefined,
    };
    const serialized = serializeMarketModel(model);

    t.deepEqual(serialized, {
        id: '123',
        categoryId: 123,
        vendorId: -1,
        vendorName: '',
        categoryName: 'test-category',
        name: 'test',
        picture: 'https://test.com',
    });
});

test('It should serialize market model without photo correctly', t => {
    const model = {
        ...rawModel,
        photo: undefined,
    };
    const serialized = serializeMarketModel(model);

    t.deepEqual(serialized, {
        id: '123',
        categoryId: 123,
        vendorId: 1234,
        vendorName: 'test-vendor',
        categoryName: 'test-category',
        name: 'test',
        picture: '',
    });
});
