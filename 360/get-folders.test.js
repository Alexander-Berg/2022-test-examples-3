'use strict';

jest.mock('../../../../schemas/folder.v1.js', () => ({
    factory: () => ({}),
    props: { id: true }
}));

const s = require('serializr');
const foldersSchema = require('./get-folders.js');
const deserialize = s.deserialize.bind(s, foldersSchema);

test('returns flattened folder list and unread messages count', () => {
    const result = deserialize({
        folders_tree: [
            {
                id: '1',
                unreadMessagesCount: 1,
                subfolders: [
                    {
                        id: '2',
                        unreadMessagesCount: 2,
                        subfolders: [
                            { id: '3', unreadMessagesCount: 3, subfolders: [] }
                        ]
                    }
                ]
            },
            { id: '4', symbol: 'spam', unreadMessagesCount: 4, subfolders: [] },
            { id: '5', symbol: 'pending', unreadMessagesCount: 4, subfolders: [] }
        ]
    });

    expect(result).toEqual({
        folders: [
            { id: '1' },
            { id: '2' },
            { id: '3' },
            { id: '4' },
            { id: '5' }
        ],
        unreadMessagesCount: 6
    });
});

test('filters symbols', () => {
    const result = deserialize({
        folders_tree: [
            {
                id: '1',
                symbol: 'pending',
                unreadMessagesCount: 1,
                subfolders: [
                    {
                        id: '2',
                        unreadMessagesCount: 2,
                        subfolders: [
                            { id: '3', unreadMessagesCount: 3, subfolders: [] }
                        ]
                    }
                ]
            },
            { id: '4', symbol: 'spam', unreadMessagesCount: 4, subfolders: [] },
            { id: '5', unreadMessagesCount: 4, subfolders: [] },
            { id: '6', symbol: 'templates', unreadMessagesCount: 4, subfolders: [] }
        ]
    }, null, { filterSymbols: [ 'pending', 'spam' ] });

    expect(result).toEqual({
        folders: [
            { id: '5' },
            { id: '6' }
        ],
        unreadMessagesCount: 8
    });
});
