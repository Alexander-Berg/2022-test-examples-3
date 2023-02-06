import { LoggerChunk, LoggerLevel } from '../types';

import { createChunk } from './create-chunk';

describe('Logger > createChunk', () => {
    test('should create raw message chunk', () => {
        expect(
            createChunk({
                level: LoggerLevel.info,
                fields: { name: 'foo' },
                parsed: {
                    message: ['message'],
                },
            }),
        ).toEqual({
            name: 'foo',
            msg: 'message',
            msgArgs: [],
            msgFormat: 'message',
            date: expect.any(Date),
            level: LoggerLevel.info,
        } as LoggerChunk);
    });
    test('should add error', () => {
        const err = new Error('foo');

        expect(
            createChunk({
                level: LoggerLevel.error,
                fields: {},
                parsed: {
                    err,
                    message: ['foo'],
                },
            }),
        ).toEqual({
            err,
            msg: 'foo',
            msgArgs: [],
            msgFormat: 'foo',
            date: expect.any(Date),
            level: LoggerLevel.error,
        } as LoggerChunk);
    });
    test('should format message with additional fields', () => {
        expect(
            createChunk({
                level: LoggerLevel.info,
                fields: { name: 'foo', foo: 'bar', msg: 'ignore me' },
                parsed: {
                    message: ['hello %s', 'world'],
                },
            }),
        ).toEqual({
            name: 'foo',
            foo: 'bar',
            msg: 'hello world',
            msgArgs: ['world'],
            msgFormat: 'hello %s',
            date: expect.any(Date),
            level: LoggerLevel.info,
        } as LoggerChunk);
    });
});
