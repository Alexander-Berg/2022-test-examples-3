'use strict';

const filter = require('./compose-autocomplete');

describe('should return empty array', function() {
    it('if data is not passed', function() {
        expect(filter()).toEqual({ items: [], autocompleteRequestId: '' });
    });

    it('if items are not passed', function() {
        expect(filter({})).toEqual({ items: [], autocompleteRequestId: '' });
    });
});

test('should return data with correct fields', function() {
    const data = {
        items: [
            {
                id: 'test_item_id',
                text: 'item_text',
                item_index: 1,
                start_index: 8,
                start_from_end_index: 1,
                text_match_length: 8,
                ngram_match_length: 0
            }
        ],
        request_id: 'test_id'
    };
    expect(filter(data)).toEqual({
        items: [
            {
                id: 'test_item_id',
                itemIndex: 1,
                name: 'item_text',
                startIndex: 8,
                startFromEndIndex: 1,
                matchLength: 8,
                ngramLength: 0
            }
        ],
        autocompleteRequestId: 'test_id'
    });
});

test('should return only first 20 items', function() {
    const data = {
        items: Array.from({ length: 30 }, (val, idx) => {
            return {
                id: `test_item_id_${idx + 1}`,
                text: `item_text_${idx + 1}`,
                item_index: idx + 1,
                start_index: idx + 5,
                text_match_length: idx + 9,
                ngram_match_length: idx
            };
        }),
        request_id: 'test_request_id'
    };

    expect(filter(data)).toEqual({
        items: Array.from({ length: 20 }, (val, idx) => {
            return {
                id: `test_item_id_${idx + 1}`,
                name: `item_text_${idx + 1}`,
                itemIndex: idx + 1,
                startIndex: idx + 5,
                matchLength: idx + 9,
                ngramLength: idx
            };
        }),
        autocompleteRequestId: 'test_request_id'
    });
});
