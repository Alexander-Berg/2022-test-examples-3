import { setCookie } from '../BeruAgeDisclaimer.utils';

describe('BeruAgeDisclaimer utils', () => {
    describe('setCookie', () => {
        it('корректно выставляет куку, значение которой должно быть escaped', () => {
            setCookie('test', '{testValue: "value"}');

            expect(document.cookie).toEqual('test=%7BtestValue%3A%20%22value%22%7D');
        });
    });
});
