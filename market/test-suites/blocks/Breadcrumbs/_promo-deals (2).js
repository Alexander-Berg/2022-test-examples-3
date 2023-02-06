import url from 'url';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Breadcrumbs на странице промо-акций магазина
 * @param {PageObject.Breadcrumbs} breadcrumbs
 */
export default makeSuite('Хлебные крошки.', {
    story: {
        'На странице промо-акций магазина': {
            'содержат ссылку на хаб скидок': makeCase({
                issue: 'MOBMARKET-10916',
                id: 'm-touch-2523',
                async test() {
                    const breadcrumbText = await this.breadcrumbs
                        .getItemByIndex(1)
                        .getText();

                    await this.expect(breadcrumbText).to.be.equal(
                        'Товары со скидкой',
                        'Текст хлебной крошки корректный'
                    );

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
