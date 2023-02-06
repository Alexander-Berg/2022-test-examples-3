'use strict';

const { expect } = require('chai');

const Filterable = require('../../../server/model/abstractions/filterable');

class TestFilterableModel extends Filterable {
    testFilter(collection) {
        return this._filterCollection(collection)
            .map(entity => entity.slug)
            .value();
    }
}

describe('Filterable model', () => {
    const mockCollection = require('./filterable-mock.json');

    function testFilter(collection, filters) {
        const testModel = new TestFilterableModel({}, filters);

        return testModel.testFilter(collection);
    }

    it('should correctly filter collection', () => {
        const filters = {
            specialization: ['Яндекс.Директ'],
            intermedia: [
                'от 5000 руб./мес.',
                'от 15000 руб.'
            ],
            audit: []
        };
        const expected = [
            'bajgolov_m',
            'baranovich-d',
            'mancevich_a',
            'shchenyova_a'
        ];
        const actual = testFilter(mockCollection, filters);

        expect(actual).to.be.deep.equal(expected);
    });

    it('should filter only by given attributes', () => {
        const filters = { metrica: ['от 500 руб.'] };
        const expected = ['zhomov_a'];
        const actual = testFilter(mockCollection, filters);

        expect(actual).to.be.deep.equal(expected);
    });

    it('should not change empty collection', () => {
        const filters = {};
        const expected = [];
        const actual = testFilter(expected, filters);

        expect(actual).to.be.deep.equal(expected);
    });
});
