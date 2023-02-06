export async function yaGetReqId(
    this: WebdriverIO.Browser,
    errorMessage: string = '',
): Promise<string> {
    return this.execute(function(errorMessage) {
        if (
            typeof window === 'undefined' ||
            typeof window.Ya === 'undefined' ||
            typeof window.Ya.Rum === 'undefined'
        ) {
            throw new Error(errorMessage || 'Невозможно получить reqid из объекта window.Ya.Rum');
        }

        // @ts-expect-error
        const reqid: string = window.Ya.Rum.getErrorSetting('reqid');
        return reqid;
    }, errorMessage);
}
