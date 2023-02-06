'use strict';

const filter = require('./compose-smartsubject');

describe('should return empty array', function() {
    it('if data is not passed', function() {
        expect(filter()).toEqual({ items: [], smartSubjectRequestId: '' });
    });

    it('if items are not passed', function() {
        expect(filter({})).toEqual({ items: [], smartSubjectRequestId: '' });
    });
});

test('should return data with correct fields', function() {
    const data = {
        items: [
            {
                id: 'test_item_id',
                text: 'item_text'
            }
        ]
    };
    expect(filter(data, 'test_id')).toEqual({
        items: [
            {
                text: 'item_text'
            }
        ],
        smartSubjectRequestId: 'test_id'
    });
});
