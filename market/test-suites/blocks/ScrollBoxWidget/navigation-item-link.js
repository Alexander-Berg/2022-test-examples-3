import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ScrollBoxWidget} scrollBox
 */
export default makeSuite('Виджет ScrollBox.', {
    story: {
        'Навигационный элемент.': {
            'Всегда': {
                'содержит ожидаемую ссылку': makeCase({
                    async test() {
                        const link = await this.scrollBoxWidget.getNavigationItemsElemLink(1);
                        await this.expect(link)
                            .to.be.link({
                                pathname: this.params.link,
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
