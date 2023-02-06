// eslint-disable-next-line valid-jsdoc
/** Достать куку, которая была перехвачена yaMockCookie */
export async function yaGetCookie(this: WebdriverIO.Browser, name: string | RegExp): Promise<string | undefined> {
    const cookies = await this.execute(() => {
        return window.mockCookies = window.mockCookies || {};
    });

    if (typeof name === 'string') {
        return cookies[name];
    }

    const cookieNames = Object.keys(cookies);
    for (let cookie of cookieNames) {
        if (name.test(cookie)) {
            return cookies[cookie];
        }
    }

    return;
}
