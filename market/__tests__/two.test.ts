describe('Two test', () => {
    const getOne = (): Promise<number> =>
        new Promise(resolve => {
            setTimeout(() => {
                resolve(1);
            }, 1000);
        });

    let a: number;

    beforeAll(() => {
        step('1 step', () => {
            a = 1;
        });

        step('2 step', () => {
            step('nested step', () => {
                a = 1;
            });
        });
    });

    afterAll(() => {
        step('some after all action', () => {
            a = 0;
        });
    });

    beforeEach(() => {
        step('some before each action', () => {
            a = 2;
        });
    });

    afterEach(() => {
        step('some after each action', () => {
            a = 0;
        });
    });

    test('numbers', async () => {
        const numberOne = await step('fetch number', () => getOne());

        step('test number', () => {
            expect(numberOne).toBe(1);
        });

        step('ok', () => {
            expect(a).toBe(2);
        });

        step('more', () => {
            expect(1).toBe(1);
            expect(2).toBe(2);
        });
    });

    test('kek', () => {
        expect(1).toBe(1);
    });
});

describe('Three test', () => {
    describe('Nested', () => {
        describe('More nested', () => {
            test('lol', () => {
                expect(1).toBe(1);
            });
        });

        test.todo('kek');
    });
});
