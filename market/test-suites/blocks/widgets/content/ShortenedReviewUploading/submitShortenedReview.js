import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ShortenedReviewUploading from '@self/platform/widgets/content/ShortenedReviewUploading/__pageObject';
import PhotoList from '@self/platform/components/ReviewForm/PhotoList/__pageObject';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';
import PersonalCabinetZeroState from '@self/platform/spec/page-objects/components/PersonalCabinetZeroState';

export default makeSuite('Оставление укороченного отзыва с фото.', {
    params: {
        text: 'Текст отзыва',
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                shortenedReviewUploading: () => this.createPageObject(ShortenedReviewUploading),
                photoList: () => this.createPageObject(PhotoList),
                ratingInput: () => this.createPageObject(RatingInput),
                personalCabinetZeroState: () => this.createPageObject(PersonalCabinetZeroState),
            });

            const {
                text,
            } = this.params;
            await this.shortenedReviewUploading.setReviewText(text);
            await this.shortenedReviewUploading.isSubmitButtonDisabled()
                .should.eventually.equal(true, 'Кнопка "Опубликовать" задизейблена');

            await this.ratingInput.setAverageGrade();
            await this.shortenedReviewUploading.isSubmitButtonDisabled()
                .should.eventually.equal(true, 'Кнопка "Опубликовать" задизейблена');

            await this.browser.yaWaitForChangeValue({
                action: () => this.photoList.choosePhoto(),
                valueGetter: () => this.shortenedReviewUploading.isSubmitButtonDisabled(),
            });
        },
        'Заполненная форма': {
            'должна раздизейблить кнопку публикации отзыва': makeCase({
                id: 'm-touch-3323',
                async test() {
                    return this.shortenedReviewUploading.isSubmitButtonDisabled()
                        .should.eventually.equal(false, 'Кнопка "Опубликовать" активна');
                },
            }),
            'должна переводить на страницу моих отзывов с параметром gradeId': makeCase({
                id: 'm-touch-3324',
                async test() {
                    const changedUrl = await this.browser.yaWaitForChangeUrl(
                        () => this.shortenedReviewUploading.submitReview());

                    return this.expect(changedUrl, 'Проверяем что URL изменился').to.be.link({
                        pathname: '/my/reviews',
                        query: {
                            gradeId: /\d+/,
                        },
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
            'должна сохранять отзыв': makeCase({
                id: 'm-touch-3325',
                async test() {
                    await this.browser.yaWaitForChangeUrl(() => this.shortenedReviewUploading.submitReview());

                    return this.personalCabinetZeroState.isVisible()
                        .should.eventually.be.equal(false, 'Zero стейт страницы моих отзывов не отображается');
                },
            }),
        },
    }),
});
