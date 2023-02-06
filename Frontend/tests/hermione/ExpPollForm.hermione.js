const assert = require('chai').assert;
const targetings = require('./fixtures/ExpPollForm-Targetings.json');

const timeout = 5000;

async function selectItems(browser, type) {
    const buttonSelector = `.ExpTolokaTargeting-Select_type_${type} .Button2`;

    await browser.click(buttonSelector);
    await browser.waitForExist('.Popup2_visible');

    await browser.click('.Popup2_visible .Menu-Item[data-key="item-0"]');

    return browser.waitUntil(async function() {
        return !(await browser.isVisible('.Popup2_visible'));
    });
}

async function selectMultiItems(browser, type, count = 2) {
    const buttonSelector = `.ExpTolokaTargeting-MultiSelect_type_${type} .Button2`;

    // открываем попап
    await browser.click(buttonSelector);
    await browser.waitForExist('.Popup2_visible');

    for (let i = 0; i < count; i++) {
        await browser.click(`.Popup2_visible .Menu-Item[data-key="item-${i}"]`);
    }

    await browser.click(buttonSelector);

    return browser.waitUntil(async function() {
        return !(await browser.isVisible('.Popup2_visible'));
    });
}

describe('ExpPollForm:', function() {
    afterEach(function() {
        return this.browser.yaCheckClientErrors();
    });

    it('проверка таргетирования', async function() {
        const browser = this.browser;
        await this.browser
            .openSbs('/experiment/115295')
            .waitForVisible('.ExpTolokaTargeting-ResetButton .Button2')
            .click('.ExpTolokaTargeting-ResetButton .Button2')
            .waitForVisible('.Modal_visible .ModalWrapper-Content .Button2_view_action')
            .click('.Modal_visible .ModalWrapper-Content .Button2_view_action');

        // Прокликиваем таргетирование
        await selectMultiItems(this.browser, 'country');
        await selectMultiItems(this.browser, 'city');
        await selectItems(this.browser, 'gender');
        await selectMultiItems(this.browser, 'os_family');
        await selectMultiItems(this.browser, 'user_agent_family');
        await selectMultiItems(this.browser, 'education');
        await selectMultiItems(this.browser, '8837');
        await selectMultiItems(this.browser, '9585');
        await selectMultiItems(this.browser, '9986');
        await selectItems(this.browser, 'prism');

        await this.browser
            .waitUntil(async function() {
                const notifications = await this.$$('.MessageList-Card.MessageCard.MessageCard_theme_green');
                return !notifications.length;
            }, 6000)
            .scroll('.ExpPollForm-SubmitGroup')
            .click('.ExpPollForm-SubmitGroup [name=save-btn]');

        await this.browser.waitForExist('.MessageList-Card.MessageCard', timeout);

        // Отправялем запрос и сохраняем его в переменную
        // так как execute синхронный
        await this.browser.execute(async function() {
            const data = await fetch('/api/experiment/115295/config');
            window.__expConfig = await data.text();
        });

        // ждём, когда в пепременной появится конфиг
        await this.browser.waitUntil(async function() {
            const { value: data } = await browser.execute(() => window.__expConfig);
            return Boolean(data);
        }, timeout);

        // читаем конфиг
        const { value: data } = await browser.execute(() => window.__expConfig);

        const config = JSON.parse(data);

        assert.deepEqual(config.toloka.targetings, targetings, JSON.stringify(config.toloka.targetings));
    });

    it('клонирование опросного эксперимента', function() {
        return this.browser
            .openSbs('/experiment/78239')
            .waitForExist('.ExpMeta-Controls', 5000)
            .click('.ExpMeta-Controls .ExpMeta-Control')
            .waitForExist('.ExpPollForm-Title')
            .waitUntil(async function() {
                const notifications = await this.$$('.MessageList-Card.MessageCard.MessageCard_theme_green');
                return !notifications.length;
            }, 6000)
            .scroll('.ExpPollForm-SubmitGroup')
            .click('.ExpPollForm-SubmitGroup [name=save-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', 5000)
            .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
            .then((title) => {
                assert.equal(title, 'Эксперимент создан');
            });
    });
});
