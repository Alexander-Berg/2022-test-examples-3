jest.disableAutomock();

import {validateWhen} from '../../contextUtils';

describe('contextUtils.validateWhen', () => {
    it('Дата не распарсена. вернет ошибку', () => {
        const when = {
            text: 'абракадабра',
        };

        const result = validateWhen(when);

        expect(result.length).toBe(1);
        expect(result).toEqual([
            {
                fields: ['when'],
                type: 'incorrect',
            },
        ]);
    });

    it('Дата пуста. Это не ошибка, т.к. в поиске будет использована дата на сегодня', () => {
        const when = {
            text: '',
        };

        expect(validateWhen(when)).toEqual([]);
    });

    it('Объект содержит дату. Ошибки нет', () => {
        const when = {
            date: '2018-10-30',
        };

        expect(validateWhen(when)).toEqual([]);
    });

    it('Объект содержит свойство special. Ошибки нет', () => {
        const when = {
            special: 'tomorrow',
        };

        expect(validateWhen(when)).toEqual([]);
    });

    it('Объект содержит день недели. Ошибки нет', () => {
        const when = {
            weekday: 0,
        };

        expect(validateWhen(when)).toEqual([]);
    });
});
