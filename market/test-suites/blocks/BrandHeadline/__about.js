import {makeCase, prepareSuite, makeSuite} from 'ginny';

import ModalSuite from '@self/platform/spec/hermione/test-suites/blocks/modal';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';


/**
 * Тест на элемент n-brand-headline__about
 * @param {PageObject.BrandHeadline} brandHeadline
 */

export default makeSuite('Кнопка "О бренде".', {
    feature: 'Фильтры',
    story: {
        beforeEach() {
            this.setPageObjects({
                modal: () => this.createPageObject(ModalFloat),
            });

            return this.brandHeadline
                .clickAbout()
                .then(() => this.modal.waitForOpened());
        },

        'При нажатии': {
            'открывает попап с информацией о бренде': makeCase({
                id: 'marketfront-902',
                test() {
                    return this.brandHeadline.popup
                        .isVisible()
                        .should.eventually.be.equal(true, 'Информация о бренде видна');
                },
            }),
        },

        'Открытый попап информации о бренде.': prepareSuite(ModalSuite),
    },
});
