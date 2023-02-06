import reviewsCountFormat from './reviews-count-format';

const testCases = [
    {
        inputCount: 1,
        output: '1 отзыв',
    },
    {
        inputCount: 2,
        output: '2 отзыва',
    },
    {
        inputCount: 5,
        output: '5 отзывов',
    },
];

describe('reviewsCountFormat', () => {
    testCases.forEach((tc) => {
        const { inputCount, output } = tc;
        const formattedReviewsCount = reviewsCountFormat(inputCount);

        test(`"${inputCount}" => "${output}"`, () => {
            expect(formattedReviewsCount).toBe(output);
        });
    });
});
