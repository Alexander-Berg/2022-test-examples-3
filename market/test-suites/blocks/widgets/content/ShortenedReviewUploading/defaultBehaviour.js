import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ShortenedReviewUploading from '@self/platform/widgets/content/ShortenedReviewUploading/__pageObject';
import PhotoList from '@self/platform/components/ReviewForm/PhotoList/__pageObject';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';

export default makeSuite('Блок оставления укороченного отзыва.', {
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                shortenedReviewUploading: () => this.createPageObject(ShortenedReviewUploading),
                photoList: () => this.createPageObject(PhotoList),
                ratingInput: () => this.createPageObject(RatingInput),
            });
        },
        'По умолчанию': {
            'форма оставления текста отзыва отображается': makeCase({
                id: 'm-touch-3319',
                async test() {
                    return this.shortenedReviewUploading.isTextFieldVisible()
                        .should.eventually.equal(true, 'Форма оставления текста отзыва видна');
                },
            }),
            'блок добавления фотографий отображается': makeCase({
                id: 'm-touch-3320',
                async test() {
                    return this.photoList.isVisible()
                        .should.eventually.equal(true, 'Блок добавления фотографий отображается');
                },
            }),
            'звёзды для оставления оценки товару отображаются': makeCase({
                id: 'm-touch-3321',
                async test() {
                    return this.ratingInput.isVisible()
                        .should.eventually.equal(true, 'Блок проставления рейтинга отображается');
                },
            }),
            'кнопка оставления отзыва задизейблена': makeCase({
                id: 'm-touch-3322',
                async test() {
                    return this.shortenedReviewUploading.isSubmitButtonDisabled()
                        .should.eventually.equal(true, 'Кнопка "Опубликовать" задизейблена');
                },
            }),
        },
    }),
});
