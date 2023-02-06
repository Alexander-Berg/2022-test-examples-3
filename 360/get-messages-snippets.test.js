'use strict';

const s = require('serializr');
const getMessagesSnippetsSchema = require('./get-messages-snippets.js');
const deserialize = s.deserialize.bind(s, getMessagesSnippetsSchema);

test('returns snippets', () => {
    const data = {
        1000: [
            {
                taksa_widget_type_1234543456546: 'snippet'
            },
            {
                taksa_widget_type_1234543456546: 'snippet-text',
                text_html: '<div>text</div>',
                text: 'text'
            },
            {
                taksa_widget_type_1234543456546: 'urls_info'
            }
        ],
        1001: [
            {
                taksa_widget_type_whatever: 'snippet'
            },
            {
                taksa_widget_type_whatever: 'snippet-text',
                text_html: '<div>text2</div>',
                text: 'text2'
            }
        ]
    };
    const result = deserialize(data, null, { messageIds: [ '1000', '1001', '1002' ] });

    expect(result).toEqual({
        messagesSnippets: [
            {
                html: '<div>text</div>',
                text: 'text'
            },
            {
                html: '<div>text2</div>',
                text: 'text2'
            },
            {
                html: '',
                text: ''
            }
        ]
    });
});
