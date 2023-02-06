describeBlock('supply__turbo-new-tab-url', block => {
    const context = {
        reqid: 'test-reqid',
        expFlags: {}
    };

    it('should return correct turbopages.org url', () => {
        assert.equal(
            block(context, {}, {
                iframeUrl: 'https://test.turbopages.org/turbo?check_swipe=1&serp-preview=1&platform=touch',
                historyStateUrl: 'https://test.turbopages.org/turbo'
            }),
            'https://test.turbopages.org/turbo?parent-reqid=test-reqid&trbsrc=wb'
        );
    });

    it('should return correct yandex.ru url', () => {
        assert.equal(
            block(context, {}, {
                iframeUrl: 'https://yandex.ru/turbo?check_swipe=1&serp-preview=1&platform=touch',
                historyStateUrl: 'https://yandex.ru/turbo'
            }),
            'https://yandex.ru/turbo'
        );
    });
});
