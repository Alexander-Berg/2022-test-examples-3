import {getGreetingWithDayTime} from './getGreetingWithDayTime';

describe('greeting', () => {
    it('getGreetingWithDayTime', () => {
        for (let i = 0; i < 6; i++) {
            expect(getGreetingWithDayTime(i)).toEqual('Доброй ночи');
        }

        for (let i = 6; i < 12; i++) {
            expect(getGreetingWithDayTime(i)).toEqual('Доброе утро');
        }

        for (let i = 12; i < 18; i++) {
            expect(getGreetingWithDayTime(i)).toEqual('Добрый день');
        }

        for (let i = 18; i < 24; i++) {
            expect(getGreetingWithDayTime(i)).toEqual('Добрый вечер');
        }
    });
});
