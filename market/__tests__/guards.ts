/* eslint-disable no-void */
import { isEmpty, isVoid } from '../guards';

describe('isVoid', () => {
    test.each`
        value           | result
        ${void 0}       | ${true}
        ${undefined}    | ${true}
        ${null}         | ${true}
        ${''}           | ${false}
        ${'some value'} | ${false}
        ${'0'}          | ${false}
        ${true}         | ${false}
        ${false}        | ${false}
        ${0}            | ${false}
        ${-0}           | ${false}
        ${[]}           | ${false}
        ${{}}           | ${false}
    `('должен вернуть $result, если передать $value', ({ value, result }) => expect(isVoid(value)).toBe(result));
});

describe('isEmpty', () => {
    test.each`
        value           | result
        ${void 0}       | ${true}
        ${undefined}    | ${true}
        ${null}         | ${true}
        ${''}           | ${true}
        ${'some value'} | ${false}
        ${'0'}          | ${false}
        ${true}         | ${false}
        ${false}        | ${false}
        ${0}            | ${false}
        ${-0}           | ${false}
        ${[]}           | ${true}
        ${[0]}          | ${false}
        ${{}}           | ${true}
        ${{ some: 0 }}  | ${false}
    `('должен вернуть $result, если передать $value', ({ value, result }) => expect(isEmpty(value)).toBe(result));
});
