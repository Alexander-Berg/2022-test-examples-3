import 'hermione';
import chai, {expect} from 'chai';
import chaiUrl from 'chai-url';

import ContentWithLabel from '../../page-objects/contentWithLabel';
import {login} from '../../helpers';
import {CASES_WITH_VALIDATION, SUCCESS_CASES} from './config';
import Button from '../../page-objects/button';
import Toast from '../../page-objects/toast';
import Error from '../../page-objects/error';

chai.use(chaiUrl);

SUCCESS_CASES.forEach(testCase => {
    describe(testCase.name, () => {
        beforeEach(function() {
            return login(testCase.url, this);
        });

        it('При успешном начислении кешбэка должен появиться зеленый тост', async function() {
            const createButton = new Button(this.browser, 'body', '[data-ow-test-toolbar-create-action="create"]');
            const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');
            const successToast = new Toast(this.browser, 'body', '[data-ow-test-toast="success"]');
            const cashbackAmount = new ContentWithLabel(
                this.browser,
                '[data-ow-test-modal-body="create"]',
                '[data-ow-test-attribute-container="cashbackAmount"]'
            );

            await createButton.isDisplayed();
            await createButton.clickButton();
            await cashbackAmount.isDisplayed();
            await cashbackAmount.setValue('100');
            await saveButton.clickButton();
            await successToast.isDisplayed();

            const isCashbackAmountInvisible = await cashbackAmount.waitForInvisible();

            expect(isCashbackAmountInvisible).to.equal(true, 'Модалка начисления кешбэка не закрылась');
        });
    });
});

CASES_WITH_VALIDATION.forEach(testCase => {
    describe(testCase.name, () => {
        beforeEach(function() {
            return login(testCase.url, this);
        });

        it('Если сумма кешбэка больше лимита, выводим сообщение об ошибке', async function() {
            const createButton = new Button(this.browser, 'body', '[data-ow-test-toolbar-create-action="create"]');
            const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');
            const error = new Error(
                this.browser,
                '[data-ow-test-modal-body="create"]',
                '[data-ow-test-global-request-error]'
            );
            const successToast = new Toast(this.browser, 'body', '[data-ow-test-toast="success"]');
            const cashbackAmount = new ContentWithLabel(
                this.browser,
                '[data-ow-test-modal-body="create"]',
                '[data-ow-test-attribute-container="cashbackAmount"]'
            );

            await createButton.isDisplayed();
            await createButton.clickButton();
            await cashbackAmount.isDisplayed();
            await cashbackAmount.setValue('10000');

            const extraErrorIsNotPresent = await error.waitForInvisible();

            expect(extraErrorIsNotPresent).to.equal(true, 'В модалке есть другие ошибки');

            await saveButton.clickButton();
            await error.isDisplayed();
            await cashbackAmount.isDisplayed();

            const isSuccessToastInvisible = await successToast.waitForInvisible();

            expect(isSuccessToastInvisible).to.equal(true, 'Валидация на начисления кешбэка не сработала');
        });
    });
});
