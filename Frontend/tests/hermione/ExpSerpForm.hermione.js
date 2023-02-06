const assert = require('chai').assert;

describe('ExpSerpForm:', function() {
    afterEach(function() {
        return this.browser.yaCheckClientErrors();
    });

    // todo понять почему на разных ветках формируется разный ключ до дампа
    // кажется, что такие тесты нужно уносить в е2е,
    // т.к. когда все ответы застаблены на файловой системе - не понятно что проверяет такой тест
    // временно заскипал
    it.skip('создание нового эксперимента', function() {
        return this.browser
            .openSbs('/experiment/create/serp')
        // Wait until everything is fancy
            .waitForExist('.ExpCreate', 5000)

        // Fill out required fields
            .setValue('[name=title-field] .textinput__control', 'E2E proclaims anarchy!')
            .setValue('[name=description-field] .textarea__control', 'I\'m not even sure if this a hermione-created task')

            .click('[name=options-field] [name=add-btn]')
            .waitForVisible('.SerpOptions-OptionForm [name=title-field]')
            .setValue('.SerpOptions-OptionForm [name=title-field] .textinput__control', 'Yandex Botnet')
            .setValue('.SerpOptions-OptionForm [name=cgi-field] .textarea__control', '&exp_flags=topkek')
            .click('.SerpOptions-OptionForm [name=save-options-btn]')

            .waitForVisible('[name=options-field] [name=add-btn]')

            .click('[name=options-field] [name=add-btn]')
            .waitForVisible('.SerpOptions-OptionForm [name=title-field]')
            .setValue('.SerpOptions-OptionForm [name=title-field] .textinput__control', 'Google Botnet')
            .setValue('.SerpOptions-OptionForm [name=cgi-field] .textarea__control', '&exp_flags=cheburek')
            .click('.SerpOptions-OptionForm [name=engine-field] .select2')
            .click('.popup2_visible_yes .menu .menu__item:first-child')
            .click('.SerpOptions-OptionForm [name=save-options-btn]')

            .scroll('[name=queries-field]')
            .setValue('[name=queries-field] .textarea__control', 'cats\ndogs\nmice\nmen')

        // Button MAY be outside of window, scroll and click "Save"
            .scroll('.ExpSerpForm-SubmitGroup')
            .click('.ExpSerpForm-SubmitGroup [name=save-btn]')

        // Wait for experiment creation
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', 5000)
            .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
            .then(function(title) {
                assert.equal(title, 'Эксперимент создан');
            });
    });

    // TODO: https://st.yandex-team.ru/SBSDEV-7790
    hermione.skip.in(/.*/, 'скипаем флапающий тест для заезда в монорепу');
    it('клонирование эксперимента', function() {
        return this.browser
            .openSbs('/experiment/1583')
            .waitForExist('.ExpMeta-Controls', 5000)
            // TODO: дать кнопке клонирования отдельный класс
            .click('.ExpMeta-Controls .ExpMeta-Control')
            .waitForExist('.ExpSerpForm-Title')
            .waitUntil(async function() {
                // ждем пока пропадут уведомления
                const notifications = await this.$$('.MessageList-Card.MessageCard.MessageCard_theme_green');
                return !notifications.length;
            }, 6000)
            .scroll('.ExpSerpForm-SubmitGroup')
            .click('.ExpSerpForm-SubmitGroup [name=save-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', 5000)
            .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
            .then((title) => {
                assert.equal(title, 'Эксперимент создан');
            });
    });
});
