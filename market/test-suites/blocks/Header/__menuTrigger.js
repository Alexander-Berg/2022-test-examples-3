import {makeSuite, makeCase} from 'ginny';
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';

/**
 * Тесты на иконку бового меню в шапке.
 * @param {PageObject.Header} header
 */
export default makeSuite('Иконка бокового меню.', {
    story: {
        beforeEach() {
            this.setPageObjects({
                sideMenu: () => this.createPageObject(SideMenu),
            });
        },

        'При нажатии': {
            'открывает боковое меню': makeCase({
                id: 'm-touch-1824',
                test() {
                    return this.header.clickMenuTrigger()
                        .then(() => this.sideMenu.waitForVisible())
                        .should.eventually.be.equal(true, 'Боковое меню отображается');
                },
            }),
        },
    },
});
