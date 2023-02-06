import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import Roll from '@self/platform/spec/page-objects/Roll';
import ContentPreview from '@self/platform/spec/page-objects/ContentPreview';

import OfferComplaintButton from '@self/platform/spec/page-objects/components/OfferComplaintButton';
import Notification from '@self/root/src/components/Notification/__pageObject';
import ComplaintForm from '@self/platform/spec/page-objects/components/ComplaintForm';
import ComplaintFormSubmitButton from '@self/platform/spec/page-objects/components/ComplaintForm/SubmitButton';
import ComplaintFormHeader from '@self/platform/spec/page-objects/components/ComplaintForm/Header';
import ProductOrOfferComplaintSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOrOfferComplaint';

/**
 * Тесты на виджет Roll/OfferComplaint
 * @param {PageObject.Roll} roll
 * @param {PageObject.ContentPreview} contentPreview
 * @param {PageObject.OfferComplaintButton} offerComplaintButton
 */
export default makeSuite('Жалоба на оффер в Roll', {
    feature: 'Жалоба в Ленте рекомендаций.',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                roll: () => this.createPageObject(Roll),
                contentPreview: () => this.createPageObject(ContentPreview),
                offerComplaintButton: () => this.createPageObject(OfferComplaintButton),
            });
            await this.browser.yaSlowlyScroll(Roll.root);
            await this.browser.scroll(Roll.root, 0, -200);
            await this.roll.clickSnippetByIndex(1);
            await this.contentPreview.isVisible()
                .should
                .eventually
                .to
                .be
                .equal(true, 'Контент превью открылся');
            await this.offerComplaintButton.clickOfferComplaintButton();
        },
        'По клику на первый сниппет': {
            'по клику на кнопку "пожаловаться" внутри сниппета': mergeSuites(
                prepareSuite(
                    ProductOrOfferComplaintSuite, {
                        params: {
                            otherIndex: 7,
                        },
                        pageObjects: {
                            notification() {
                                return this.createPageObject(Notification);
                            },
                            complaintForm() {
                                return this.createPageObject(ComplaintForm);
                            },
                            complaintFormSubmitButton() {
                                return this.createPageObject(ComplaintFormSubmitButton);
                            },
                            complaintFormHeader() {
                                return this.createPageObject(ComplaintFormHeader);
                            },
                        },
                    }
                )),
        },
    },
});
