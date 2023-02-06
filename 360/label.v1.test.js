'use strict';

jest.mock('../i18n');

const s = require('serializr');
const mockI18n = require('../i18n');
const labelSchema = require('./label.v1.js');

describe('socialLabelSchema', () => {
    const args = { lang: 'ru' };
    const deserialize = s.deserialize.bind(null, labelSchema.social);

    it('copies properties', () => {
        const result = deserialize({ id: 'foo' });
        expect(result).toEqual({ id: 'foo' });
    });

    describe('type', () => {
        it('returns type', () => {
            const result = deserialize({ type: { title: 'system' } });
            expect(result.type).toBe('system');
        });
    });

    describe('name', () => {
        it('returns localized label name', () => {
            mockI18n.get.mockReturnValue('Facebook.com');
            const result = deserialize({ name: 'facebook' }, null, args);
            expect(mockI18n.get).toBeCalledWith('ru', 'social_labels', 'facebook');
            expect(result.name).toBe('Facebook.com');
        });

        it('omits name property', () => {
            const result = deserialize({ name: 'unknown' }, null, args);
            expect(result).not.toHaveProperty('name');
        });
    });

    describe('symbol', () => {
        it('returns label symbol', () => {
            const result = deserialize({ name: 'facebook' }, null, args);
            expect(result.symbol).toBe('facebook');
        });
    });
});

describe('systemLabelSchema', () => {
    const args = { lang: 'ru' };
    const deserialize = s.deserialize.bind(null, labelSchema.system);

    it('copies properties', () => {
        const result = deserialize({
            id: 'foo',
            messagesCount: 1
        });
        expect(result).toEqual({
            id: 'foo',
            messagesCount: 1
        });
    });

    describe('type', () => {
        it('returns type', () => {
            const result = deserialize({ type: { title: 'system' } });
            expect(result.type).toBe('system');
        });
    });

    describe('name', () => {
        it('returns localized label name', () => {
            mockI18n.get.mockReturnValue('Важные');
            const result = deserialize({ name: 'foo', symbolicName: { title: 'important_label' } }, null, args);
            expect(mockI18n.get).toBeCalledWith('ru', 'labels', 'important_label');
            expect(result.name).toBe('Важные');
        });

        it('omits name property', () => {
            const result = deserialize({ name: 'foo', symbolicName: { title: 'seen_label' } }, null, args);
            expect(result).not.toHaveProperty('name');
        });
    });

    describe('symbol', () => {
        it('returns label symbol', () => {
            const result = deserialize({ symbolicName: { title: 'foo' } }, null, args);
            expect(result.symbol).toBe('foo');
        });
    });
});

describe('userLabelSchema', () => {
    const deserialize = s.deserialize.bind(null, labelSchema.user);

    it('copies properties', () => {
        const result = deserialize({
            id: 'foo',
            name: 'bar',
            messagesCount: 1
        });
        expect(result).toEqual({
            id: 'foo',
            name: 'bar',
            messagesCount: 1
        });
    });

    describe('type', () => {
        it('returns type', () => {
            const result = deserialize({ type: { title: 'user' } });
            expect(result.type).toBe('user');
        });
    });

    describe('color', () => {
        it('returns color in hex format', () => {
            const result = deserialize({ color: '4095' });
            expect(result.color).toBe('#000fff');
        });

        it('returns black', () => {
            const result = deserialize({ color: '0' });
            expect(result.color).toBe('#000000');
        });

        it('returns default color', () => {
            const result = deserialize({ color: '' });
            expect(result.color).toBe('#31c73b');
        });
    });

    describe('createDate', () => {
        it('returns creation date in iso format', () => {
            const result = deserialize({ creationTime: '1528899671' });
            expect(result.createDate).toBe('2018-06-13T14:21:11.000Z');
        });
    });
});
