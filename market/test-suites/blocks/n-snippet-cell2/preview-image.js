import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-snippet-cell2
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Превью изображения продукта.', {
    feature: 'Гридовый сниппет продукта.',
    story: {
        'По умолчанию': {
            'должно показываться ожидаемое превью изображения продукта': makeCase({
                environment: 'kadavr',
                issue: 'MARKETVERSTKA-28692',
                id: 'marketfront-1393',
                params: {
                    previewImageUrl: 'Ожидаемый url превью изображения продукта',
                },
                async test() {
                    await this.snippetCell2.image.isExisting();
                    const imageSrc = await this.snippetCell2.getImageSrc();

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
                    const snippet = this.snippetCell2;
                    await snippet.imagesPagination.isExisting();
                    let visible = await snippet.imagesPagination.isVisible();
                    await this.expect(visible).equal(false);
                    await snippet.hoverGallerySnippet();
                    visible = await snippet.imagesPagination.isVisible();
                    await this.expect(visible).equal(true);
                },
            }),
            'должен менять дефолтное превью при наведении на пагинационное меню': makeCase({
                environment: 'kadavr',
                id: 'marketfront-4227',
                issue: 'MARKETFRONT-23857',
                params: {
                    countImages: 'Кол-во ожидаемых фотографий',
                },
                async test() {
                    const snippet = this.snippetCell2;
                    await snippet.imagesPagination.isExisting();
                    const oldImageSrc = await snippet.getImageSrc();
                    await snippet.getPaginationItemButtonFocus(2);
                    const newImageSrc = await snippet.getImageSrc();
                    await this.expect(oldImageSrc).not.equal(newImageSrc);
                },
            }),
        },
    },
});
