const chai = require('chai');
const mocha = require('mocha');
const describe = mocha.describe;
const it = mocha.it;
const expect = chai.expect;

import { GetLeaferButtons } from '../../../src/components/Leafer/GetLeaferButtons';
import { EXPECTED } from './testData';

describe('Тесты Leafer\'a', () => {
    describe('Проверка кнопок', () => {
        const CANONIZED = {};
        Object.keys(EXPECTED).forEach(i => {
            CANONIZED[`${i}`] = {};
            Object.keys(EXPECTED[i]).forEach(j => {
                const actual = GetLeaferButtons(Number(i), Number(j));
                CANONIZED[`${i}`][`${j}`] = actual;
                it(`Всего страниц: ${i}, активная: ${j}`, () => {
                    // expect({a: 1}).to.deep.equal({a: 1});
                    expect(actual).to.deep.equal(EXPECTED[i][j]);
                    // assert.equal(actual, EXPECTED[i][j]);\
                });
            });
        });
    // console.log("CANONIZED: ", JSON.stringify(CANONIZED, null, 3));
    });
});
