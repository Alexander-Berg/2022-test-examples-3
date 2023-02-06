import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Stories} stories
 * @param {PageObject.GalleryModal} galleryModal
 * @param {PageObject.GalleryModalStory} galleryModalStory
 */
export default makeSuite('Блок сториз с фотками из отзывов пользователей.', {
    story: {
        'По умолчанию': {
            'Отображается на странице': makeCase({
                id: 'm-touch-3803',
                issue: 'MARKETFRONT-5999',
                test() {
                    return this.stories.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),
        },
        'При клике на сниппет': {
            'Открывается сториз': makeCase({
                id: 'm-touch-2958',
                issue: 'MARKETFRONT-5998',
                async test() {
                    await this.stories.clickItem();

                    return this.galleryModal
                        .isExisting()
                        .should.eventually.to.be.equal(true, 'Сториз открылась');
                },
            }),
        },
        'При клике на сториз из 2 фоток от одного юзера': {
            'Фотка меняется, а userId нет': makeCase({
                id: 'm-touch-3805',
                issue: 'MARKETFRONT-5997',
                async test() {
                    await this.stories.clickItem();
                    const firstUrl = await this.galleryModalStory.getSrcAttributeForPicture();
                    const userId = await this.galleryModalStory.getUserId();

                    await this.galleryModalStory.clickNext();

                    await this.galleryModalStory.getUserId()
                        .should.eventually.to.be.equal(userId, 'Фотки должны быть от одного юзера');

                    return this.galleryModalStory.getSrcAttributeForPicture()
                        .should.eventually.not.to.be.equal(firstUrl, 'Фотки не должны совпадать');
                },
            }),
        },
        'При переключении сториз от одного юзера': {
            'Меняется фотка и userId': makeCase({
                id: 'm-touch-3806',
                issue: 'MARKETFRONT-5996',
                async test() {
                    await this.stories.clickItem();
                    const firstUrl = await this.galleryModalStory.getSrcAttributeForPicture();
                    const userId = await this.galleryModalStory.getUserId();

                    await this.galleryModalStory.clickNext();
                    await this.galleryModalStory.clickNext();

                    this.galleryModalStory.getUserId()
                        .should.eventually.not.to.be.equal(userId, 'Фотки должны быть от разных юзеров');

                    return this.galleryModalStory.getSrcAttributeForPicture()
                        .should.eventually.not.to.be.equal(firstUrl, 'Фотки не должны совпадать');
                },
            }),
        },
        'Ссылка "Перейти к отзыву"': {
            'Содержит нужный reviewId, productId и slug': makeCase({
                id: 'm-touch-3807',
                issue: 'MARKETFRONT-5995',
                async test() {
                    await this.stories.clickItem();

                    return this.galleryModalStory.getReviewLink()
                        .should.eventually.be.link({
                            pathname: `product--${this.params.slug}/${this.params.productId}/reviews/${this.params.reviewId}`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
