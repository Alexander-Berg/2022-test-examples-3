import BeruDisclaimerAdapter from '../BeruDisclaimer.adapter';

let instance: BeruDisclaimerAdapter;

describe('BeruDisclaimerAdapter', () => {
    describe('Метод transform', () => {
        beforeEach(() => {
            // @ts-ignore
            instance = new BeruDisclaimerAdapter({});
        });

        describe('возрастной дисклеймер', () => {
            ageAssert(['age_0'], 0);
            ageAssert(['age_6', 'age_5', 'age_4', 'age_3', 'age_2', 'age_1'], 6);
            ageAssert(['age_12', 'age_11', 'age_10', 'age_9', 'age_8', 'age_7'], 12);
            ageAssert(['age_16', 'age_15', 'age_14', 'age_13'], 16);
            ageAssert(['adult', 'age', 'age_18', 'age_17'], 18);
        });

        describe('дисклеймер опасных препаратов', () => {
            it('должен корректно возвращать входные значения', () => {
                const expectedText = 'длинное название';

                [
                    'drugs', 'drugs_with_delivery', 'medicine',
                    'medicine_bad', 'medicine_recipe', 'medicine_specification',
                    'supplements', 'tobacco', 'zoo_medicine',
                ].forEach((type: string) => {
                    const props = instance.transform({
                        block: 'block',
                        warning: {
                            type,
                            value: {
                                full: expectedText,
                                short: 'короткое название',
                            },
                        },
                    });

                    expect(props).toEqual({
                        isMedicine: true,
                        text: expectedText,
                    });
                });
            });
        });

        describe('базовый дисклеймер', () => {
            it('должен корректно возвращать входные значения', () => {
                const expectedText = 'длинное название';
                const props = instance.transform({
                    block: 'block',
                    warning: {
                        type: 'other-type',
                        value: {
                            full: expectedText,
                            short: 'короткое название',
                        },
                    },
                });

                expect(props).toEqual({
                    text: expectedText,
                });
            });
        });

        it('должен возвращать даные как есть, если нет warning', () => {
            const props = instance.transform({
                block: 'block',
                age: '18',
                text: 'текст',
            });

            expect(props).toEqual({
                age: '18',
                text: 'текст',
            });
        });
    });
});

function ageAssert(types: string[], expectedAge: 0 | 6 | 12 | 16 | 18) {
    it(`должен корректно возвращать входные значения для ${expectedAge}+`, () => {
        const expectedText = 'длинное название';

        types.forEach((type: string) => {
            const props = instance.transform({
                block: 'block',
                warning: {
                    type,
                    value: {
                        full: expectedText,
                        short: 'короткое название',
                    },
                },
            });

            expect(props).toEqual({
                age: expectedAge.toString(),
                text: expectedText,
            });
        });
    });
}
