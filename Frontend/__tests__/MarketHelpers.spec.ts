import { getReviewsNumberText } from '@yandex-turbo/components/MarketHelpers/MarketHelpers';

describe('MarketHelpers', () => {
    it('getReviewsNumberText должен рендерится без ошибок', () => {
        expect(getReviewsNumberText(45321)).toEqual('45K отзывов');
        expect(getReviewsNumberText(1000)).toEqual('1K отзывов');
        expect(getReviewsNumberText(22)).toEqual('22 отзыва');
        expect(getReviewsNumberText(11)).toEqual('11 отзывов');
        expect(getReviewsNumberText(1)).toEqual('1 отзыв');
    });
});
