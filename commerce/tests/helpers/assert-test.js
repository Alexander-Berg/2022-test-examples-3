const catchError = require('tests/helpers/catchError').func;
const assert = require('helpers/assert');
const { expect } = require('chai');

describe('Assert helper', () => {
    describe('`isNumber`', () => {
        it('should throw error when argument not number', () => {
            const isNumber = assert.isNumber.bind(assert, 'abc', 400, 'shortMessage', 'SHT', { a: 'b' });
            const error = catchError(isNumber);

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('shortMessage');
            expect(error.options).to.deep.equal({ internalCode: '400_SHT', a: 'b' });
        });

        it('should success when argument is number', () => {
            assert.isNumber(123);
        });
    });

    describe('`isSlug`', () => {
        it('should throw error when argument not valid slug', () => {
            const isSlug = assert.isSlug.bind(assert, '!@#', 400, 'shortMessage', 'SHT', { a: 'b' });
            const error = catchError(isSlug);

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('shortMessage');
            expect(error.options).to.deep.equal({ internalCode: '400_SHT', a: 'b' });
        });

        it('should success when argument is slug', () => {
            assert.isSlug('correct_Slug-1');
        });
    });

    describe('`isOpenId`', () => {
        it('should throw error when argument not valid openId', () => {
            const isOpenId = assert.isOpenId.bind(assert, 'open-!@#-id', 400, 'shortMessage', 'SHT', { a: 'b' });
            const error = catchError(isOpenId);

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('shortMessage');
            expect(error.options).to.deep.equal({ internalCode: '400_SHT', a: 'b' });
        });

        it('should success when argument is openId', () => {
            assert.isOpenId('f81d4fae-7dec-11d0-a765-00a0c91e6bf6');
        });
    });

    describe('`isBoolean`', () => {
        it('should throw error when argument is not boolean', () => {
            const isBoolean = assert.isBoolean.bind(assert, 'not_boolean', 400, 'shortMessage', 'SHT', { a: 'b' });
            const error = catchError(isBoolean);

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('shortMessage');
            expect(error.options).to.deep.equal({ internalCode: '400_SHT', a: 'b' });
        });

        it('should success when argument is boolean', () => {
            assert.isBoolean(true);
            assert.isBoolean(false);
        });
    });
});
