import {makeSuite, makeCase} from 'ginny';

import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import PhotoList from '@self/platform/spec/page-objects/components/PhotoList';
import Tooltip from '@self/platform/spec/page-objects/levitan-gui/Tooltip';

/**
 * Тесты на блок n-review-form
 * при загрузке фотографии больше допустимого размера
 * и при единовременном добавлении более 15 фотографий
 *
 * @param {PageObject.ReviewForm} reviewForm
 * @param {PageObject.ReviewFormProduct} reviewFormProduct
 */

export default makeSuite('Форма отзыва на товар. Добавление отзыва с фотографиями.', {
    feature: 'Оставление отзыва с фото',
    environment: 'kadavr',
    issue: 'MARKETVERSTKA-23313',
    story: {
        beforeEach() {
            this.setPageObjects({
                imagePreview: () => this.createPageObject(PhotoList),
                ratingStars: () => this.createPageObject(RatingStars, {parent: ProductReviewForm.rating}),
                tooltip: () => this.createPageObject(Tooltip),
            });

            const schema = {
                mdsPictures: [
                    {
                        namespace: 'market-ugc',
                        groupId: 3723,
                        imageName: '2a000001654282aec0648192ce44a1708325',
                    },
                ],
            };

            return this.browser.setState('schema', schema)
                .then(() => this.browser.yaScenario(this, 'productReviews.fillForm.firstStep'));
        },

        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        'При загрузке фото размером более 10МБ': {
            'получаем сообщение с ошибкой и фото не загружается': makeCase({
                issue: 'MARKETVERSTKA-30545',
                id: 'marketfront-514',
                test() {
                    return this.browser.allure.runStep(
                        'Добавляем к отзыву фотографию больше 10МБ',
                        () => this.reviewForm.photosControlSelector
                            .then(inputSelector =>
                                this.browser.chooseFile(inputSelector, `${__dirname}/large.jpeg`)
                            )
                    )
                        .then(() => this.browser.allure.runStep(
                            'Проверяем появление попапа об ошибке загрузки фотографии',
                            () => this.tooltip.waitForContentVisible()
                                .should.eventually.equal(true, 'Появился попап с ошибкой')
                                .then(() => this.tooltip.getContentText())
                                .should.eventually.equal(
                                    'К одному отзыву можно добавить до 15 фотографий. ' +
                                        'Максимальный объём одной фотографии — 10 МБ.'
                                )
                        ))
                        .then(() => this.browser.allure.runStep(
                            'Проверяем, что фотография не загрузилась',
                            () => this.imagePreview.isUploaderPreviewExisting()
                                .should.eventually.equal(false, 'Фотография не загружена в отзыв')
                        ))
                        .then(() => this.reviewForm.submitFirstStep())
                        .then(() => this.reviewForm.submitSecondStep());
                },
            }),
        },

        'При попытке загрузки более 15 фотографий': {
            'получаем сообщение с ошибкой и последнее фото не загружается': makeCase({
                id: 'marketfront-515',
                issue: 'MARKETVERSTKA-30650',
                environment: 'kadavr',
                test() {
                    return this.browser.allure.runStep(
                        'Добавляем к отзыву 15 фотографий',
                        () => uploadNImages.call(this, 15))

                        .then(() => this.browser.allure.runStep(
                            'Проверяем, что они успешно прицепились к отзыву',
                            () => this.imagePreview.isExactPhotoPreviewExisting(15)
                                .should.eventually.equal(true, 'Все фотографии успешно загружены в отзыв')
                        ))

                        .then(() => this.browser.allure.runStep(
                            'Загружаем еще одну лишнюю (16-ю) фотографию',
                            () => uploadNImages.call(this, 1))
                        )

                        .then(() => this.browser.allure.runStep(
                            'Проверяем появление попапа об ошибке загрузки фотографии',
                            () => this.tooltip.waitForContentVisible()
                                .should.eventually.equal(true, 'Появился попап с ошибкой')
                                .then(() => this.tooltip.getContentText())
                                .should.eventually.equal(
                                    'К одному отзыву можно добавить до 15 фотографий. ' +
                                        'Максимальный объём одной фотографии — 10 МБ.'
                                )
                        ))

                        .then(() => this.browser.allure.runStep(
                            'Проверяем, что лишняя фотография не прицепилась к отзыву',
                            () => this.imagePreview.isExtraPhotoPreviewExisiting()
                                .should.eventually.equal(false, 'Фотография не загружена в отзыв')
                        ))
                        .then(() => this.reviewForm.submitFirstStep())
                        .then(() => this.reviewForm.submitSecondStep());
                },
            }),
        },
    },
});

/**
 * Рекурсивно прикрепляет к отзыву n-ое количество фотографий
 *
 * @param {Number} nPics количество фотографий, которое хотим загрузить
 * @return {Promise}
 */
function uploadNImages(nPics) {
    if (nPics === 0) {
        return Promise.resolve(nPics);
    }

    return new Promise(resolve => {
        this.reviewForm.photosControlSelector
            .then(inputSelector => this.browser.chooseFile(inputSelector, `${__dirname}/normal.jpg`))
            .then(() => uploadNImages.call(this, nPics - 1))
            .then(() => resolve(nPics));
    });
}
