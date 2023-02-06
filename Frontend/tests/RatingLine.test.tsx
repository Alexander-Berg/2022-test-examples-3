import { getLinkForReviewsByProductId } from '../utils/getLinkForReviewsByProductId';

describe('RatingLine', () => {
    it('генерируется ссылка на отзывы', () => {
        const result = getLinkForReviewsByProductId('845909010');
        expect(result).toBe('https://market.yandex.ru/product/845909010/reviews');
    });
});
