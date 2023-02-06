import {makeSuite, makeCase} from 'ginny';
import url from 'url';

import {getTUSAccount, unlockTUSAccount, TUS_CONSUMER, TUS_META_NAME} from '@self/root/src/spec/hermione/plugins/tus';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import {filterBackendLogs} from '@self/project/src/spec/hermione/helpers/getBackendRequestParams';

export default makeSuite('Слияние корзин', {
    id: 'marketfront-5819',
    story: {
        async beforeEach() {
            this.setPageObjects({
                loginButton: () => this.createPageObject(Button, {
                    parent: this.modal,
                    root: `[data-auto="checkout-auth-continue"] ${Button.root}`,
                }),
            });

            await this.cartCheckoutButton.waitForButtonEnabled();
            await this.cartCheckoutButton.goToCheckout();
            await this.modal.waitForVisible();
        },
        'При залогине корзин вызывается метод картера для слияния корзин': makeCase({
            async test() {
                await this.browser.yaWaitForChangeUrl(() => this.loginButton.click());
                const {login, password, uid} = await getTUSAccount.call(this, TUS_CONSUMER, {
                    tags: 'no-lock',
                    lock_duration: 0,
                }).then(json => json.account);

                const cookies = await this.browser.getCookie(this);
                const yandexUidCookie = cookies.find(cookie => cookie.name === 'yandexuid');
                await this.browser.setMeta(TUS_META_NAME, {login, uid, auth: true});

                const loginInput = await this.browser.element('[data-t="field:input-login"]');
                await this.browser.setValue(loginInput.selector, login);

                const nextButton = await this.browser.element('[data-t="button:action:passp:sign-in"]');
                await this.browser.click(nextButton.selector);

                const passwordInput = await this.browser.element('[data-t="field:input-passwd"]');
                await this.browser.setValue(passwordInput.selector, password);

                await this.browser.yaSafeAction(
                    this.browser.yaWaitForChangeUrl(() => this.browser.click(nextButton.selector), 2000),
                    false
                );

                const logs = await filterBackendLogs(this, 'Carter');
                const mergeRequest = logs
                    .filter(log => (log.request.method === 'PATCH'))
                    .map(log => url.parse(log.request.url, true))
                    .find(request => request.pathname === `/cart/YANDEXUID/${yandexUidCookie.value}/list`);

                await this.expect(mergeRequest).to.be.not.equal(
                    undefined,
                    'Ресурс мерджа корзин отсутствует'
                );
                await this.browser.allure.runStep(
                    'Вызов ресурса мерджа корзин произведен с правильными параметрами',
                    async () => {
                        await this.expect(mergeRequest.query.idTo).to.be.equal(uid);
                        await this.expect(mergeRequest.query.typeTo).to.be.equal('UID');
                    }
                );
            },
        }),
        async afterEach() {
            await unlockTUSAccount.call(this);
        },
    },
});
