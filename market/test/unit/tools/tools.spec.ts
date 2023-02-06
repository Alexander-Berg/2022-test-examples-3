import tools from '../../../src/tools';

const { pluralize, priceAnalyze, getISBN, getRatingReviewsText, cleanText, makeHashCode } = tools;

describe('tools:', () => {
    describe('pluralize', () => {
        const forms = ['отзыв', 'отзыва', 'отзывов'];
        const testData = [
            {
                actualReviewCount: 1,
                expectedResult: 'отзыв',
            },
            {
                actualReviewCount: 2,
                expectedResult: 'отзыва',
            },
            {
                actualReviewCount: 5,
                expectedResult: 'отзывов',
            },
            {
                actualReviewCount: 13,
                expectedResult: 'отзывов',
            },
            {
                actualReviewCount: 21,
                expectedResult: 'отзыв',
            },
            {
                actualReviewCount: 22,
                expectedResult: 'отзыва',
            },
            {
                actualReviewCount: 25,
                expectedResult: 'отзывов',
            },
            {
                actualReviewCount: 100,
                expectedResult: 'отзывов',
            },
            {
                actualReviewCount: 264,
                expectedResult: 'отзыва',
            },
            {
                actualReviewCount: 267,
                expectedResult: 'отзывов',
            },
            {
                actualReviewCount: 1,
                expectedResult: '1 отзыв',
                combine: true,
            },
            {
                actualReviewCount: 2,
                expectedResult: '2 отзыва',
                combine: true,
            },
            {
                actualReviewCount: 5,
                expectedResult: '5 отзывов',
                combine: true,
            },
            {
                actualReviewCount: 13,
                expectedResult: '13 отзывов',
                combine: true,
            },
            {
                actualReviewCount: 21,
                expectedResult: '21 отзыв',
                combine: true,
            },
            {
                actualReviewCount: 22,
                expectedResult: '22 отзыва',
                combine: true,
            },
            {
                actualReviewCount: 25,
                expectedResult: '25 отзывов',
                combine: true,
            },
            {
                actualReviewCount: 100,
                expectedResult: '100 отзывов',
                combine: true,
            },
            {
                actualReviewCount: 264,
                expectedResult: '264 отзыва',
                combine: true,
            },
            {
                actualReviewCount: 267,
                expectedResult: '267 отзывов',
                combine: true,
            },
        ];

        testData.forEach(({ actualReviewCount, expectedResult, combine = false }) => {
            it(`'${actualReviewCount}' => '${expectedResult}'`, () => {
                expect(expectedResult).toEqual(pluralize(forms, actualReviewCount, combine));
            });
        });
    });

    describe('price analyze:', () => {
        const testData = [
            {
                actualPriceString: '1 420',
                expectedResult: '1420',
            },
            {
                actualPriceString: '3.490 3.598',
                expectedResult: '3490',
            },
            {
                actualPriceString: '55 990 руб.',
                expectedResult: '55990',
            },
            {
                actualPriceString: '12/12.02/12.000.000 (belarus) asd',
                expectedResult: '12',
            },
            {
                actualPriceString: '123.56',
                expectedResult: '124',
            },
            {
                actualPriceString: '123.12',
                expectedResult: '123',
            },
            {
                actualPriceString: '',
                expectedResult: '',
            },
            {
                actualPriceString: 'abc',
                expectedResult: '',
            },
        ];

        testData.forEach(({ actualPriceString, expectedResult }) => {
            it(`'${actualPriceString}' => '${expectedResult}'`, () => {
                expect(priceAnalyze(actualPriceString)).toEqual(expectedResult);
            });
        });
    });

    describe('priceAnalyze (price range):', () => {
        beforeEach(() => {
            window.document.domain = 'aliexpress.ru';
        });

        const testData = [
            {
                actualPriceString: '3 893,95 - 5 142,91 руб.',
                expectedResult: 3894,
            },
        ];

        testData.forEach(({ actualPriceString, expectedResult }) => {
            it(`'${actualPriceString}' => '${expectedResult}'`, () => {
                expect(priceAnalyze(actualPriceString)).toEqual(expectedResult);
            });
        });
    });

    describe('getISBN method:', () => {
        it('works properly with a given ISBN number', () => {
            const isbnString = '978-5-271-39378-5';
            const testString = 'Steve Jobs by Walter Isaacson | 978-5-271-39378-5';
            expect(getISBN(testString)).toEqual(isbnString);
        });

        it('works properly when title contains 2 ISBN numbers', () => {
            const isbnString = '978-5-271-39378-5,978-5-271-39378-7';
            const testString = 'Steve Jobs by Walter Isaacson | 978-5-271-39378-5, 978-5-271-39378-7';
            expect(getISBN(testString)).toEqual(isbnString);
        });

        it('works properly when title doesn\'t have an ISBN number', () => {
            const testString = 'Steve Jobs by Walter Isaacson';
            expect(getISBN(testString)).toEqual('');
        });

        it('works properly when title contains an ISBN number and other digits', () => {
            const isbnString = '978-5-271-39378-5';
            const testString = 'Steve Jobs by Walter Isaacson, September 15, 2015 | 978-5-271-39378-5';
            expect(getISBN(testString)).toEqual(isbnString);
        });
    });

    describe('getRatingReviewsText method:', () => {
        const ratingValue = 5;
        const basePhrase = `Рейтинг ${ratingValue} из 5 на основе`;
        const defaultText = 'оценки службы контроля качества Яндекс.Маркета';
        const testData = [
            {
                reviewsCount: 0,
                expectedResult: defaultText,
            },
            {
                reviewsCount: 10,
                expectedResult: '10 отзывов',
            },
        ];

        testData.forEach(({ reviewsCount, expectedResult }) => {
            it(`'${basePhrase} ${expectedResult}'`, () => {
                expect(getRatingReviewsText(reviewsCount, defaultText)).toEqual(expectedResult);
            });
        });
    });

    describe('getTextContents method:', () => {
        const testData = [
            // $€₽
            {
                inputData: '<div><span>STRING</span></div>',
                expectedResult: 'STRING',
            },
            {
                inputData: '   ₽        ',
                expectedResult: '₽',
            },
            {
                inputData: '<div><img alt="IMAGE" src="test.png"></div>',
                expectedResult: 'IMAGE',
                useAlt: true,
            },
            {
                inputData: '   ₽     $  <div><span>1</span></div>   €   ',
                expectedResult: '₽ $ 1 €',
            },
            {
                inputData: '<div><img alt="IMAGE" src="test.png"></div>',
                expectedResult: '',
            },
        ];

        testData.forEach(({ inputData, expectedResult, useAlt }) => {
            it(`should get text from '${inputData}' as '${expectedResult}'`, () => {
                const element = window.document.createElement('DIV');
                element.innerHTML = inputData;

                expect(tools.getTextContents(element, useAlt)).toEqual(expectedResult);
            });
        });
    });

    describe('cleanText method: ', () => {
        it('shouldn\'t change empty string', () => {
            const testString = '';
            const expectedString = '';

            expect(cleanText(testString)).toEqual(expectedString);
        });

        it('should leave valid symbols', () => {
            const testString = '* Капсульная кофемашина DeLonghi Nespresso Vertuo Next ENV120.BW,, коричневый *';
            const expectedString = 'КапсульнаякофемашинаDeLonghiNespressoVertuoNextENV120.BW,,коричневый';

            expect(cleanText(testString)).toEqual(expectedString);
        });

        it('shouldn\'t change string with all valid symbols', () => {
            const testString = 'Кофемашина';
            const expectedString = 'Кофемашина';

            expect(cleanText(testString)).toEqual(expectedString);
        });

        it('should remove all unvalid symbols', () => {
            const testString = '!@#$%^&*()+= ?:;';
            const expectedString = '';

            expect(cleanText(testString)).toEqual(expectedString);
        });
    });

    describe('makeHashCode method: ', () => {
        it('should return 0 for empty string', () => {
            const testString = '';
            const expectedResult = 0;

            expect(makeHashCode(testString)).toEqual(expectedResult);
        });

        it('should return the same hashes for same strings', () => {
            const testString = 'Капсульная кофемашина DeLonghi Nespresso Vertuo Next ENV120.BW,, коричневый';

            expect(makeHashCode(testString)).toEqual(makeHashCode(testString));
        });

        it('should return different hashes for different strings', () => {
            const firstTestString = 'Капсульная кофемашина DeLonghi Nespresso Vertuo Next ENV120.BW,, коричневыйа';
            const secondTestString = 'Капсульная кофемашина DeLonghi Nespresso Vertuo Next ENV120.BW,, синий';

            expect(makeHashCode(firstTestString)).not.toEqual(makeHashCode(secondTestString));
        });

        it('should return hash for string', () => {
            const testString = 'Капсульная кофемашина DeLonghi Nespresso Vertuo Next ENV120.BW,, коричневый';

            expect(makeHashCode(testString)).not.toEqual(0);
        });
    });

    describe('tools.getDataByPath method: ', () => {
        it('should return value by index', () => {
            const input = { items: [{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }] };
            const testingPath = 'items[2].a';
            const expectedResult = 4;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return array of value for each item', () => {
            const input = { items: [{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }] };
            const testingPath = 'items[].a';
            const expectedResult = JSON.stringify([1, 2, 4]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array by key', () => {
            const input = { items: [{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }] };
            const testingPath = 'items';
            const expectedResult = JSON.stringify([{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array by key with []', () => {
            const input = { items: [{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }] };
            const testingPath = 'items[]';
            const expectedResult = JSON.stringify([{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return empty array', () => {
            const input = {
                items: [{ a: 1, d: 7 }, { b: 2, a: 2, d: { f: 5 } }, { a: 4, d: { m: 6 } }, { c: 4 }],
            };
            const testingPath = 'items[].a.d';
            const expectedResult = JSON.stringify([]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return null for empty json', () => {
            const input = {};
            const testingPath = 'items[].a';
            const expectedResult = null;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return initial object for undefined path', () => {
            const input = { items: [{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }] };
            const testingPath = undefined;
            const expectedResult = input;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should throw error and return null for wrong path', () => {
            const input = { items: [{ a: 1 }, { b: 2, a: 2 }, { a: 4 }, { c: 4 }] };
            const testingPath = 'items[4].d';
            const expectedResult = null;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return array for path without indexes', () => {
            const input = {
                items: [
                    { a: 1, b: 5 },
                    { a: 2, d: { b: [{ c: 4 }, { k: 2, c: 7 }] } },
                ],
            };
            const testingPath = 'items[].d.b[].c';
            const expectedResult = JSON.stringify([4, 7]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array for path with index', () => {
            const input = {
                items: [
                    { a: 1, b: 5 },
                    { a: 2, d: { b: [{ c: 4 }, { k: 2, c: 7 }] } },
                ],
            };
            const testingPath = 'items[].d.b[0].c';
            const expectedResult = JSON.stringify([4]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return value for path with right indexes', () => {
            const input = {
                items: [
                    { a: 1, b: 5 },
                    { a: 2, d: { b: [{ c: 4 }, { k: 2, c: 7 }] } },
                ],
            };
            const testingPath = 'items[1].d.b[0].c';
            const expectedResult = 4;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should throw error and return null for path with wrong indexes', () => {
            const input = {
                items: [
                    { a: 1, b: 5 },
                    { a: 2, d: { b: [{ c: 4 }, { k: 2, c: 7 }] } },
                ],
            };
            const testingPath = 'items[0].d.b[0].c';
            const expectedResult = null;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return array', () => {
            const input = {
                items: [
                    { a: 1, b: 5, d: { b: [{ f: 4 }, { k: 2, c: 7 }] } },
                    { a: 2, d: { b: [{ f: 4 }, { k: 2, c: 8 }] } },
                ],
            };
            const testingPath = 'items[].d.b[].c';
            const expectedResult = JSON.stringify([7, 8]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return empty array', () => {
            const input = {
                items: [
                    { a: 1, b: 5, d: { b: [{ f: 4 }, { k: 2, c: 7 }] } },
                    { a: 2, d: { b: [{ f: 4 }, { k: 2, c: 8 }] } },
                ],
            };
            const testingPath = 'items[].d.b[0].c';
            const expectedResult = JSON.stringify([]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array with wrapped in array data', () => {
            const input = [{ a: 1 }, { a: 2, b: 3, d: [{ f: 5 }, { f: 6 }] }, { c: 4 }];
            const testingPath = 'd[].f';
            const expectedResult = JSON.stringify([5, 6]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array with wrapped in array data and with indexes', () => {
            const input = [{ a: 1 }, { a: 2, b: 3, d: [{ f: 5 }, { f: 6 }] }, { c: 4 }];
            const testingPath = 'd[1].f';
            const expectedResult = JSON.stringify([6]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array with wrapped in array data and first array in path', () => {
            const input = [{ a: 1, d: [{ f: 7 }, { f: 8 }] }, { a: 2, b: 3, d: [{ f: 5 }, { f: 6 }] }, { c: 4 }];
            const testingPath = '[].d[].f';
            const expectedResult = JSON.stringify([7, 8, 5, 6]);

            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return array with wrapped in array data and first array in path', () => {
            const input = [{ a: 1, d: [{ f: 7 }, { f: 8 }] }, { a: 2, b: 3, d: [{ f: 5 }, { f: 6 }] }, { c: 4 }];
            const testingPath = '[0].d[].f';
            const expectedResult = JSON.stringify([7, 8]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });

        it('should return null for number', () => {
            const input = 5;
            const testingPath = '[0].d[].f';
            const expectedResult = null;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return null for string', () => {
            const input = 'str';
            const testingPath = '[0].d[].f';
            const expectedResult = null;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return null for boolean', () => {
            const input = true;
            const testingPath = '[0].d[].f';
            const expectedResult = null;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return initial value for null with empty path', () => {
            const input = null;
            const testingPath = '';
            const expectedResult = input;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return initial value for number with empty path', () => {
            const input = 5;
            const testingPath = '';
            const expectedResult = input;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return initial value for string with empty path', () => {
            const input = 'str';
            const testingPath = '';
            const expectedResult = input;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return initial value for boolean with empty path', () => {
            const input = true;
            const testingPath = '';
            const expectedResult = input;

            expect(tools.getDataByPath(input, testingPath)).toEqual(expectedResult);
        });

        it('should return array with value by found selector in array', () => {
            const input = {
                items: [
                    {
                        a: [
                            { id: 123, b: 100 },
                            { id: 321, b: 1 },
                        ],
                    },
                    {
                        a: [
                            { id: 123, b: 110 },
                            { id: 321231, b: 1 },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a{id=$path}.b';
            const selector = { $path: 123 };
            const expectedResult = JSON.stringify([100, 110]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath, selector))).toEqual(expectedResult);
        });

        it('should return array with value by selector in element of array', () => {
            const input = {
                items: [
                    {
                        a: [
                            { id: 123, b: 100 },
                            { id: 321, b: 1 },
                        ],
                    },
                    {
                        a: [
                            { id: 321, b: 110 },
                            { id: 123, b: 1 },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a[0]{id=$path}.b';
            const selector = { $path: 123 };
            const expectedResult = JSON.stringify([100]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath, selector))).toEqual(expectedResult);
        });

        it('should return array with value by 2 selector in array', () => {
            const input = {
                items: [
                    {
                        a: [
                            { id: 123, b: { innerId: 321, c: 10 } },
                            { id: 321, b: { innerId: 123, c: 101 } },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a{id=$path}.b{innerId=$innerPath}.c';
            const selector = { $path: 123, $innerPath: 321 };
            const expectedResult = JSON.stringify([10]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath, selector))).toEqual(expectedResult);
        });

        it('should return array with value by 2 selector in element of array', () => {
            const input = {
                items: [
                    {
                        a: [
                            {
                                id: 123,
                                b: [
                                    { innerId: 1, c: 5 },
                                    { innerId: 2, c: 10 },
                                ],
                            },
                            {
                                id: 321,
                                b: [
                                    { innerId: 3, c: 15 },
                                    { innerId: 4, c: 20 },
                                ],
                            },
                        ],
                    },
                    {
                        a: [
                            {
                                id: 321,
                                b: [
                                    { innerId: 5, c: 25 },
                                    { innerId: 6, c: 30 },
                                ],
                            },
                            {
                                id: 123,
                                b: [
                                    { innerId: 7, c: 35 },
                                    { innerId: 8, c: 40 },
                                ],
                            },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a[1]{id=$path}.b[1]{innerId=$innerPath}.c';
            const selector = { $path: 321, $innerPath: 4 };
            const expectedResult = JSON.stringify([20]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath, selector))).toEqual(expectedResult);
        });

        it('should return empty array with wrong path in {}', () => {
            const input = {
                items: [
                    {
                        a: [
                            { id: 123, b: 100 },
                            { id: 321, b: 1 },
                        ],
                    },
                    {
                        a: [
                            { id: 123, b: 110 },
                            { id: 321231, b: 1 },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a{id=$path}.b';
            const selector = { $path: 100 };
            const expectedResult = JSON.stringify([]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath, selector))).toEqual(expectedResult);
        });

        it('should return empty array with incorrect selector', () => {
            const input = {
                items: [
                    {
                        a: [
                            { id: 123, b: 100 },
                            { id: 321, b: 1 },
                        ],
                    },
                    {
                        a: [
                            { id: 123, b: 110 },
                            { id: 321231, b: 1 },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a{id=$path}.b';
            const selector = { $notPath: 100 };
            const expectedResult = JSON.stringify([]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath, selector))).toEqual(expectedResult);
        });

        it('should return array when passing value in {}', () => {
            const input = {
                items: [
                    {
                        a: [
                            { id: 123, b: 100 },
                            { id: 321, b: 1 },
                        ],
                    },
                    {
                        a: [
                            { id: 123, b: 110 },
                            { id: 321231, b: 1 },
                        ],
                    },
                ],
            };
            const testingPath = 'items[].a{id=123}.b';
            const expectedResult = JSON.stringify([100, 110]);
            expect(JSON.stringify(tools.getDataByPath(input, testingPath))).toEqual(expectedResult);
        });
    });

    describe('tools.filterBySelector method: ', () => {
        it('should return initial data with correct reference in selector', () => {
            const input = { id: 100, data: 'abc' };
            const expectedResult = JSON.stringify(input);
            const selector = 'id=$path';
            const values = { $path: 100 };

            expect(JSON.stringify(tools.filterBySelector(input, selector, values))).toEqual(expectedResult);
        });

        it('should return initial data with correct value in selector ', () => {
            const input = { id: 100, data: 'abc' };
            const expectedResult = JSON.stringify(input);
            const selector = 'id=100';

            expect(JSON.stringify(tools.filterBySelector(input, selector))).toEqual(expectedResult);
        });

        it('should return undefined with incorrect reference in selector ', () => {
            const input = { id: 100, data: 'abc' };
            const expectedResult = undefined;
            const selector = 'id=$path';
            const values = { $notPath: 100 };

            expect(tools.filterBySelector(input, selector, values)).toEqual(expectedResult);
        });

        it('should return undefined with incorrect value in selector ', () => {
            const input = { id: 100, data: 'abc' };
            const expectedResult = undefined;
            const selector = 'id=200';

            expect(tools.filterBySelector(input, selector)).toEqual(expectedResult);
        });

        it('should return undefined with incorrect value in selector ', () => {
            const input = { id: 100, data: 'abc' };
            const expectedResult = undefined;
            const selector = '=100';

            expect(tools.filterBySelector(input, selector)).toEqual(expectedResult);
        });

        it('should return undefined with {} input', () => {
            const input = {};
            const expectedResult = undefined;
            const selector = 'id=100';

            expect(tools.filterBySelector(input, selector)).toEqual(expectedResult);
        });

        it('should return undefined with undefined input ', () => {
            const input = undefined;
            const expectedResult = undefined;
            const selector = 'id=100';

            expect(tools.filterBySelector(input, selector)).toEqual(expectedResult);
        });

        it('should return undefined with [] input', () => {
            const input: unknown[] = [];
            const expectedResult = undefined;
            const selector = 'id=100';

            expect(tools.filterBySelector(input, selector)).toEqual(expectedResult);
        });
    });

    describe('tools.quantityAnalyze method: ', () => {
        it('should return correct int number from string', () => {
            const input = '10 шт.';
            const expectedResult = 10;

            expect(tools.quantityAnalyze(input)).toEqual(expectedResult);
        });

        it('should return correct float number from string', () => {
            const input = '10,23 кг.';
            const expectedResult = 10.23;

            expect(tools.quantityAnalyze(input)).toEqual(expectedResult);
        });

        it('should return correct 1 from string "Последний"', () => {
            const input = 'Последний';
            const expectedResult = 1;

            expect(tools.quantityAnalyze(input)).toEqual(expectedResult);
        });

        it('should return correct 0 from string "Нет в наличии"', () => {
            const input = 'Нет в наличии';
            const expectedResult = 0;

            expect(tools.quantityAnalyze(input)).toEqual(expectedResult);
        });
    });
});
