describe('webdriver.io', () => {
    it('Telemost Tests', () => {
        $('//button[contains(@title, \'микрофон\')]').click();
        browser.pause(1000);
        browser.sessionId = 123456;
    })
})
