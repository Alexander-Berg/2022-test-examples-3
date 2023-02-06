const config = require('../skipped.js');

const expectNotToBeEmptyString = value => {
    expect(typeof value === 'string' || value instanceof String).toBe(true);
    expect(value.length).toBeGreaterThan(0);
};

describe('Конфигурационный объект skipped.js', () => {
    it('Должен парситься в json без ошибок', () => expect(() => JSON.stringify(config)).not.toThrow());

    describe('Должен соответствовать структуре', () => {
        const skipLists = Object.values(config);

        skipLists.forEach(list => {
            list.forEach(({issue, reason, cases}) => {
                it('поле issue должно быть нашим', () => expect(issue).toMatch(/^MARKETPARTNER-\d+$/));

                it(`для issue ${issue} поле reason должно быть строкой`, () => {
                    expect(reason).toEqual(expect.any(String));
                });

                it(`для issue ${issue} поле cases не должно быть пустым массивом`, () => {
                    expect(cases instanceof Array).toBe(true);
                    expect(cases.length).toBeGreaterThan(0);
                });

                describe(`для issue ${issue} каждый элемент внутри cases`, () => {
                    cases.forEach(({id, fullName}) => {
                        it('должен иметь заполненное поле id', () => expectNotToBeEmptyString(id));
                        it(`должен иметь поле id (${id}), соответствующие схеме markembi-[id]`, () =>
                            expect(id).toMatch(/^(marketmbi-\d*)$/));
                        it('должен иметь заполненное поле fullName', () => expectNotToBeEmptyString(fullName));
                    });
                });
            });
        });
    });
});
