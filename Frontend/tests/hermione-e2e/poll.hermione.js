const assert = require('chai').assert;
const { getTextWithDate } = require('./utils');
const delay = require('./delay-config');
const LOGIN = process.env.LOGIN;
const PASSWORD = process.env.PASS;

describe('Опросы:', function() {
    it('создание опроса', function() {
        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/create/poll')
            .waitForExist('.ExpCreate', delay.ROOT_MOUNT)
            .setValue('[name=title-field] input', getTextWithDate('E2E proclaims anarchy!'))
            .click('.PollForm-Add .button2')
            .waitForExist('.PollForm-AddButton:first-child', delay.USER_ACTION)
            .click('.PollForm-AddButton:first-child')
            .waitForExist('.PollForm-Item_type_text textarea', delay.USER_ACTION)
            .setValue('.PollForm-Item_type_text textarea', 'This is the Hermione\'s text')
            .click('.PollForm-Add .button2')
            .waitForExist('.PollForm-AddButton:nth-child(5)', delay.USER_ACTION)
            .click('.PollForm-AddButton:nth-child(5)')
            .waitForExist('.PollForm-Item_type_question textarea', delay.USER_ACTION)
            .setValue('.PollForm-Item_type_question textarea', 'This is the Hermione\'s question')
            .click('[name=save-and-start-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', delay.NIRVANA)
            .getText('.MessageList-Card.MessageCard.MessageCard_theme_green .MessageBox-Text')
            .then((title) => assert.equal(title, 'Эксперимент запущен'));
    });

    it('клонирование опросного эксперимента', function() {
        return this.browser
            .passportAuth(LOGIN, PASSWORD)
            .url('/experiment/44985')
            .waitForExist('.ExpMeta-Controls', delay.ROOT_MOUNT)
            .click('.ExpMeta-Controls .ExpMeta-Control')
            .waitForExist('.PollForm-Add', delay.USER_ACTION)
            .click('.PollForm-Add .dropdown2')
            .click('.PollForm-AddButton .PollForm-TypeIcon_type_radio')
            .waitForExist('.PollForm-Item_type_radio textarea', delay.USER_ACTION)
            .setValue('.PollForm-Item_type_radio textarea', 'This is the Hermione\'s radio textarea')
            .setValue('.PollForm-Option:first-child input', '1')
            .setValue('.PollForm-Option:nth-child(2) input', '2')
            .click('[name=save-btn]')
            .waitForExist('.MessageList-Card.MessageCard.MessageCard_theme_green', delay.NIRVANA);
    });
});
