import url from 'url';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Breadcrumbs на странице промо-акций магазина
 * @param {PageObject.Breadcrumbs} breadcrumbs
 */
export default makeSuite('Хлебные крошки.', {
    story: {
        'На странице промо-акций магазина': {
            'отображает корректный текст': makeCase({
                async test() {
                    const breadcrumbText = await this.breadcrumbs
                        .getItemByIndex(1)
                        .getText();

                    return this.expect(breadcrumbText).to.be.equal(
                        'Товары со скидкой',
                        'Текст хлебной крошки корректный'
                    );
                },
            }),

            'имеет ссылку на хаб скидок': makeCase({
                issue: 'MARKETVERSTKA-31509',
                id: 'marketfront-2923',
                async test() {
                    const breadcrumbUrl = await this.breadcrumbs
                        .getItemByIndex(1)
                        .getAttribute('href');

                    const {pathname} = url.parse(breadcrumbUrl);

                    return this.expect(pathname).to.equal(
                        '/catalog--tovary-so-skidkoi/61522',
                        'Адрес ссылки корректный'
                    );
                },
            }),
        },
    },
});
