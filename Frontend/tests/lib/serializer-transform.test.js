const assert = require('assert');

const serializer = require('lib/serializer');
const transformByPath = require('lib/serializer/transform');

describe('Serialize library', () => {
    it('should serialize object', () => {
        const data = {
            name: 'Bamboo',
            id: '1111',
            description: 'Something green',
            inner: { description: 'A plant' },
        };

        const transform = {
            name: 'name',
            id: 'id',
            description: 'inner.description',
        };

        const actual = transformByPath(transform, data);
        const expected = {
            name: 'Bamboo',
            id: '1111',
            description: 'A plant',
        };

        assert.deepStrictEqual(actual, expected);
    });

    it('should serialize through function', () => {
        const data = {
            name: 'Bamboo',
            id: '1111',
            description: 'Something green',
            inner: { description: 'A plant' },
        };
        const transform = {
            name: 'name',
            description: origData =>
                `${data.description} and ${origData.inner.description.toLowerCase()}`,
        };

        const actual = transformByPath(transform, data);
        const expected = {
            name: 'Bamboo',
            description: 'Something green and a plant',
        };

        assert.deepEqual(actual, expected);
    });

    it('should serialize with deep properties set', () => {
        const data = {
            name: 'Bamboo',
            id: '1111',
            description: 'Something green',
        };
        const transform = {
            name: 'name',
            'outer.description': 'description',
        };

        const actual = transformByPath(transform, data);
        const expected = {
            name: 'Bamboo',
            outer: {
                description: 'Something green',
            },
        };

        assert.deepEqual(actual, expected);
    });

    it('should serialize nested object', () => {
        const data = {
            event: {
                id: 1,
                title: 'title',
            },
        };
        const transform = {
            event: {
                id: 'id',
                title: 'title',
            },
        };

        const actual = transformByPath(transform, data);

        assert.deepEqual(actual, data);
    });

    it('should serialize with custom serializer', () => {
        const data = {
            event: {
                id: 1,
                title: 'title',
            },
        };
        const transform = ({ event }) => ({
            eventId: event.id,
            eventTitle: event.title,
        });

        const actual = serializer(transform)(data);
        const expected = {
            eventId: 1,
            eventTitle: 'title',
        };

        assert.deepEqual(actual, expected);
    });
});
