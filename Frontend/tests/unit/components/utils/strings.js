const { calcLines, calcStringWeight, plural, numToChar, removeBlankLines, findUUIDs, deleteDuplicates } = require('../../../../src/client/components/utils/strings');

describe('components/utils/strings', () => {
    describe('calcLines', () => {
        it('should return a Number', () => {
            assert(typeof calcLines('s1\ns2') === 'number');
        });

        it('should calc lines properly', () => {
            assert.equal(calcLines('s1\ns2\ns3'), 3);
            assert.equal(calcLines(''), 0);
            assert.equal(calcLines('s1'), 1);
        });
    });

    describe('calcStringWeight', () => {
        it('should return a Number', () => {
            assert(typeof calcStringWeight('some string') === 'number');
        });

        it('should calc string weight properly', () => {
            assert.equal(calcStringWeight('a'), 1);
            assert.equal(calcStringWeight('2a'), 2);
            assert.equal(calcStringWeight('D\td\n'), 4);
        });
    });

    describe('plural', () => {
        const cases = {
            0: 'котов',
            1: 'кот',
            2: 'кота',
            3: 'кота',
            5: 'котов',
            6: 'котов',
            10: 'котов',
            11: 'котов',
            13: 'котов',
            21: 'кот',
            22: 'кота',
            25: 'котов',
        };

        Object.keys(cases).forEach((key) => {
            it(`для ${key} должна формироваться правильная форма`, () => {
                assert.equal(plural(Number(key), ['кот', 'кота', 'котов']), cases[key]);
            });
        });
    });

    describe('numToChar', () => {
        const unicodeOffset = 1040;
        const unicodeSize = 32;

        it('0 = А', () => {
            assert.equal(numToChar(unicodeOffset, unicodeSize, 0), 'А');
        });

        it('31 = Я', () => {
            assert.equal(numToChar(unicodeOffset, unicodeSize, 31), 'Я');
        });

        it('32 = АА', () => {
            assert.equal(numToChar(unicodeOffset, unicodeSize, 32), 'АА');
        });

        it('1056 = ААА', () => {
            assert.equal(numToChar(unicodeOffset, unicodeSize, 1056), 'ААА');
        });

        it('33147 = ЯКЫ', () => {
            assert.equal(numToChar(unicodeOffset, unicodeSize, 33147), 'ЯКЫ');
        });

        it('Latin, 18277 = ZZZ', () => {
            assert.equal(numToChar(65, 26, 18277), 'ZZZ');
        });
    });

    describe('removeBlankLines', () => {
        it('должен убрать пустые строки', () => {
            assert.equal(removeBlankLines(
                'обеспечительный платеж 213\n' +
                'сзади 172\n' +
                '\n' +
                '\n'), 'обеспечительный платеж 213\n' +
                'сзади 172');
        });
    });

    describe('findUUIDs', () => {
        it('для ссылки с двумя UUID должны отдаваться оба в правильном порядке', () => {
            assert.deepEqual(findUUIDs(
                'https://nirvana.yandex-team.ru/flow/1579d7cd-0847-4b5a-aeb2-fb852706eba1/8f470326-7e6c-492e-8b01-d05aaef9300f/graph'),
            [
                '1579d7cd-0847-4b5a-aeb2-fb852706eba1',
                '8f470326-7e6c-492e-8b01-d05aaef9300f',
            ]);
        });

        it('для ссылки с одним UUID должен отдаваться только один', () => {
            assert.deepEqual(findUUIDs(
                'https://nirvana.yandex-team.ru/flow/1579d7cd-0847-4b5a-aeb2-fb852706eba1'),
            ['1579d7cd-0847-4b5a-aeb2-fb852706eba1'],
            );
        });

        it('для просто строки с одним UUID должен возвращать его же, обёрнутый в массив', () => {
            assert.deepEqual(findUUIDs(
                '1579d7cd-0847-4b5a-aeb2-fb852706eba1'),
            ['1579d7cd-0847-4b5a-aeb2-fb852706eba1'],
            );
        });

        it('для строки в котором нет UUID должен возвращать пустой массив', () => {
            assert.deepEqual(findUUIDs('Absolutely no UUID in this string'), []);
        });

        it('для пустой строки должен возвращать пустой массив', () => {
            assert.deepEqual(findUUIDs(''), []);
        });
    });

    describe('deleteDuplicates', () => {
        it('должен убрать дублирующие строки', () => {
            const s = 'яндекс 4\nгугл 4\nамазон 4\nмайкрасофт\nяндекс 4\nяндекс 4\nяндекс 4\nяндекс 4';
            const expectedValue = 'яндекс 4\nгугл 4\nамазон 4\nмайкрасофт';

            assert.equal(deleteDuplicates(s), expectedValue);
        });
    });
});
