'use strict';

const s = require('serializr');
const popFoldersSchema = require('./get-pop-folders.js');
const deserialize = s.deserialize.bind(s, popFoldersSchema);

describe('folderIds', () => {
    it('returns folder IDs with enabled POP', () => {
        const result = deserialize({
            folders: {
                1: { pop3On: '1' },
                2: { pop3On: '0' },
                3: { pop3On: '1' }
            }
        });

        expect(result).toEqual({
            folderIds: [ '1', '3' ]
        });
    });
});
