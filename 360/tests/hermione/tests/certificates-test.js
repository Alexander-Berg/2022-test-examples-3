describe('certificates ->', () => {
    /**
     * У яндексового грида нет наших сертификатов, поэтому сначала запускается просто этот тест
     * и выставляет сертификат (доверять этому сайту), после чего уже запускаются остальные тесты.
     * Доверие сертификатам ставится самой гермионой.
     */
    it('set certificate', async function() {
        await this.browser.url();
        await this.browser.pause(1000);
    });
});