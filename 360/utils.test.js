'use strict';

const { folderTypeFromSymbolAndType, labelTypeFromSymbolAndType } = require('../build/utils/utils');

describe('v3/utils folders', () => {
    it('should return user type regardless of symbol if the type is user', () => {
        expect(folderTypeFromSymbolAndType('archive', 'user')).toEqual('user');
    });
    it('should return Archive type if symbol is Archive', () => {
        expect(folderTypeFromSymbolAndType('archive', 'system')).toEqual('archive');
    });
    it('should return Inbox type if symbol is Inbox', () => {
        expect(folderTypeFromSymbolAndType('inbox', 'system')).toEqual('inbox');
    });
    it('should return Trash type if symbol is Trash', () => {
        expect(folderTypeFromSymbolAndType('trash', 'system')).toEqual('trash');
    });
    it('should return Sent type if symbol is Sent', () => {
        expect(folderTypeFromSymbolAndType('sent', 'system')).toEqual('sent');
    });
    it('should return Drafts type if symbol is Draft', () => {
        expect(folderTypeFromSymbolAndType('draft', 'system')).toEqual('drafts');
    });
    it('should return Outgoing type if symbol is Outbox', () => {
        expect(folderTypeFromSymbolAndType('outbox', 'system')).toEqual('outgoing');
    });
    it('should return Templates type if symbol is Template', () => {
        expect(folderTypeFromSymbolAndType('template', 'system')).toEqual('templates');
    });
    it('should return Discounts type if symbol is Discount', () => {
        expect(folderTypeFromSymbolAndType('discount', 'system')).toEqual('discounts');
    });
    it('should return Unsubscribe type if symbol is Unsubscribe', () => {
        expect(folderTypeFromSymbolAndType('unsubscribe', 'system')).toEqual('unsubscribe');
    });
});

describe('v3/utils labels', () => {
    it('should return user type regardless of symbol if the type is user', () => {
        expect(labelTypeFromSymbolAndType('important_label', 'user')).toEqual('user');
    });
    it('should return Important type if symbol is important_label', () => {
        expect(labelTypeFromSymbolAndType('important_label', 'system')).toEqual('important');
    });
    it('should return Read type if symbol is seen_label', () => {
        expect(labelTypeFromSymbolAndType('seen_label', 'system')).toEqual('read');
    });
    it('should return Attach type if symbol is attached_label', () => {
        expect(labelTypeFromSymbolAndType('attached_label', 'system')).toEqual('attach');
    });
});
