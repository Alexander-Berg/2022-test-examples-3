require('dotenv').config();

module.exports = async function beforeEachCallback() {
    const { ROBOT_LOGIN, ROBOT_PASSWORD_VAULT } = require('../constants/robot-bobot-passport.js');

    const { browser } = this;
    const robotPassword = await browser.getSecrets(
        process.env.BOBOT_TOKEN,
        ROBOT_PASSWORD_VAULT,
    );

    await browser.passportLogin(ROBOT_LOGIN, robotPassword);
    await browser.pause(1000);
};
