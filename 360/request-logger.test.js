'use strict';

jest.mock('@yandex-int/duffman');

const { Logger } = require('@yandex-int/duffman');
const RequestLogger = require('./request-logger.js');

describe('request-logger', () => {
    it('overrides _prepareArgs', () => {
        const logger = new RequestLogger();

        expect(logger).toBeInstanceOf(Logger);
        expect(logger._prepareArgs('foo', 'bar')).toBe('bar');
    });
});
