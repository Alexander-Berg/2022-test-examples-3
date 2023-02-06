import highspeed from '../../highSpeedTrain';

const options = [
    {
        value: '1',
        text: 'Ласточка',
    },
    {
        value: '2',
        text: 'Воробей',
    },
    {
        value: '3',
        text: 'Цапля',
    },
];

describe('highspeedTrains', () => {
    describe('validateValue', () => {
        it('Вернёт исходный список, если все его элементы представлены в опциях фильтра', () => {
            const value = ['1', '2'];

            expect(highspeed.validateValue(value, options)).toEqual(value);
        });

        it('Отфильтрует значения не представленные в опциях фильтра', () => {
            const value = ['-1', '0', '1'];

            expect(highspeed.validateValue(value, options)).toEqual(['1']);
        });

        it('Вернёт пустой список, если ни один из элементов не представлен в опциях фильтра', () => {
            const value = ['-123'];

            expect(highspeed.validateValue(value, options)).toEqual(
                highspeed.getDefaultValue(),
            );
        });
    });
});
