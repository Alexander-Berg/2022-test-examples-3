function setUserAgent(userAgent: string) {
    Object.defineProperty(window.navigator, 'userAgent', {
        value: userAgent,
        writable: true,
    });
}

describe('userAgent', () => {
    const originalUserAgent = window.navigator.userAgent;

    beforeEach(() => jest.resetModules());
    afterEach(() => setUserAgent(originalUserAgent));

    describe('isBuggyIos12', () => {
        it('isBuggyIos12 == true на iOS 12', async() => {
            setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1');

            const { isBuggyIos12 } = await import('../userAgent');

            expect(isBuggyIos12).toBeTruthy();
        });

        it('isBuggyIos12 == true на iOS 12.1', async() => {
            setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 12_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1');

            const { isBuggyIos12 } = await import('../userAgent');

            expect(isBuggyIos12).toBeTruthy();
        });

        it('isBuggyIos12 == true на iOS 12.3', async() => {
            setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 12_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.3.1 Mobile/15E148 Safari/604.1');

            const { isBuggyIos12 } = await import('../userAgent');

            expect(isBuggyIos12).toBeTruthy();
        });

        it('isBuggyIos12 == false на iOS 12.4', async() => {
            setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 EdgiOS/45.5.0 Mobile/15E148 Safari/605.1.15');

            const { isBuggyIos12 } = await import('../userAgent');

            expect(isBuggyIos12).toBeFalsy();
        });

        it('isBuggyIos12 == false на iOS другой версии', async() => {
            setUserAgent('Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_2 like Mac OS X) AppleWebKit/603.2.4 (KHTML, like Gecko) Version/10.0 Mobile/14F89 Safari/602.1');

            const { isBuggyIos12 } = await import('../userAgent');

            expect(isBuggyIos12).toBeFalsy();
        });

        it('isBuggyIos12 == false на другом устройстве', async() => {
            const { isBuggyIos12 } = await import('../userAgent');

            expect(isBuggyIos12).toBeFalsy();
        });
    });
});
