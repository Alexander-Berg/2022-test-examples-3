import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-card2
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Превью изображения продукта.', {
    feature: 'Листовой сниппет продукта.',
    story: {
        'По умолчанию': {
            'должно показываться ожидаемое превью изображения продукта': makeCase({
                issue: 'MARKETVERSTKA-28692',
                id: 'marketfront-1393',
                environment: 'kadavr',
                params: {
                    previewImageUrl: 'Ожидаемый url превью изображения продукта',
                },
                async test() {
                    await this.snippetCard2.image.isExisting();
                    const imageSrc = await this.snippetCard2.getSrcAttributeForImage();

                    return this.browser.allure.runStep('На сниппете присутствует ожидаемое изображение продукта',
                        () => this.expect(imageSrc).to.be.link(this.params.previewImageUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
        'При наведении': {
            'должен отображаться пагинатор': makeCase({
                environment: 'kadavr',
                id: 'marketfront-4226',
                issue: 'MARKETFRONT-23857',
                params: {
                    countImages: 'Кол-во ожидаемых фотографий',
                },
                async test() {
                    const snippet = this.snippetCard2;
                    await snippet.imagesPagination.isExisting();
                    let visible = await snippet.imagesPagination.isVisible();
                    await this.expect(visible).equal(false);
                    await snippet.hoverGallerySnippet();
                    visible = await snippet.imagesPagination.isVisible();
                    await this.expect(visible).equal(true);
                },
            }),
            'должен менять дефолтную превью при наведении на пагинационного меню': makeCase({
                environment: 'kadavr',
                id: 'marketfront-4227',
                issue: 'MARKETFRONT-23857',
                params: {
                    countImages: 'Кол-во ожидаемых фотографий',
                },
                async test() {
                    const snippet = this.snippetCard2;
                    await snippet.imagesPagination.isExisting();
                    const oldImageSrc = await snippet.getImageSrc();
                    await snippet.getPaginationItemButtonFocus(2);
                    const newImageSrc = await snippet.getImageSrc();
                    await this.expect(oldImageSrc).not.equal(newImageSrc);
                },
            }),
            'должен возвращать на дефолтную при перевода фокуса на другой товар': makeCase({
                environment: 'kadavr',
                id: 'marketfront-4228',
                issue: 'MARKETFRONT-23857',
                params: {
                    countImages: 'Кол-во ожидаемых фотографий',
                },
                async test() {
                    const snippet = this.snippetCard2;
                    await snippet.imagesPagination.isExisting();
                    const oldImageSrc = await snippet.getImageSrc();
                    await snippet.getPaginationItemButtonFocus(2);
                    const newImageSrc = await snippet.getImageSrc();
                    await this.expect(oldImageSrc).not.equal(newImageSrc);

                    const snippet2 = this.anotherCard;
                    await snippet2.imagesPagination.isExisting();
                    await snippet2.getPaginationItemButtonFocus(1);
                    const afterUnFocusImageSrc = await snippet.getImageSrc();
                    await this.expect(oldImageSrc).equal(afterUnFocusImageSrc);
                },
            }),
        },
    },
});
