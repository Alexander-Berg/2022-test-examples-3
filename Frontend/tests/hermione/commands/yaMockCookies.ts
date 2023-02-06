// eslint-disable-next-line valid-jsdoc
/** Перехват куки на этапе проставления для дальнейшего прочтения yaGetCookie */
export async function yaMockCookies(this: WebdriverIO.Browser) {
    await this.execute(() => {
        window.mockCookies = window.mockCookies || {};
        Object.defineProperty(document, 'cookie', {
            set: cookie => {
                const [name, value] = cookie.split('=');
                window.mockCookies[name] = value;
            },
        });
    });
}
