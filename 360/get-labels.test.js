'use strict';

jest.mock('../../../../schemas/label.v1.js');

const s = require('serializr');
const labelsSchema = require('./get-labels.js');
const deserialize = s.deserialize.bind(s, labelsSchema);

test('returns labels', () => {
    const data = {
        labels: {
            1: { type: { title: 'system' } },
            2: { type: { title: 'user' } },
            3: { type: { title: 'unknown' } }
        }
    };
    const args = {
        labelTypes: [ 'system', 'user' ]
    };
    const result = deserialize(data, null, args);

    expect(result).toEqual({
        labels: [
            { id: '1', type: { title: 'system' } },
            { id: '2', type: { title: 'user' } }
        ]
    });
});
