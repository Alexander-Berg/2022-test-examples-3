'use strict';

const parseAttachments = require('./parse-attachments.js');
const mbodyMock = require('../../../../test/mock/disk/mbody.json');

describe('parseAttachments', () => {
    it('happy path', () => {
        expect(parseAttachments(mbodyMock.attachments)).toMatchSnapshot();
    });

    it('без параметра -> []', () => {
        expect(parseAttachments()).toEqual([]);
    });
});
