import {prepareSuite, mergeSuites, makeSuite, makeCase} from 'ginny';

// suites
import ModalSuite from '@self/platform/spec/hermione/test-suites/blocks/modal';
import OfferInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-offer-info';
// page-objects
import OfferInfo from '@self/platform/widgets/content/OfferDetailsPopup/__pageObject';
// Это последний импорт этого PO, не забудь удалить сам PO после обновления теста
import ProductContentBlock from '@self/platform/spec/page-objects/n-product-content-block';
import Modal from '@self/platform/spec/page-objects/modal';

/**
 * Тесты на блок n-i-offer-details-button.
 * @param {PageObject.OfferDetailButton} offerDetailButton
 */
export default makeSuite('Блок показа дополнительной информации об офере.', {
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    modal: () => this.createPageObject(Modal),
                });

                return this.offerDetailButton.showDetail();
            },

            'По клику': {
                'должен открывать попап "I" с информацией об офере': makeCase({
                    id: 'marketfront-945',
                    feature: 'Попапи',
                    test() {
                        return this.modal.wrapper
                            .isExisting(OfferInfo.root)
                            .should.eventually.to.be.equal(true, 'Попап содержит информацию об оффере');
                    },
                }),
            },
        },

        prepareSuite(ModalSuite),

        prepareSuite(OfferInfoSuite, {
            pageObjects: {
                offerInfo() {
                    return this.createPageObject(OfferInfo);
                },
                productContentBlock() {
                    return this.createPageObject(ProductContentBlock, {
                        parent: this.modal.wrapper,
                    });
                },
            },
        })
    ),
});
