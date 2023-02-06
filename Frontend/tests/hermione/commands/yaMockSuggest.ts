export type mockData = [string, string, Record<string, unknown>][];

const EMPTY_RESPONSE: mockData = [['', '', { suggestions: [] }]];

/**
 * Подменяет запрос саджеста
 */
export async function yaMockSuggest(
    this: WebdriverIO.Browser,
    text: string,
    mock: mockData = EMPTY_RESPONSE
): Promise<void> {
    await this.execute((text: string, mock: mockData) => {
        const { MBEM } = window;

        window.MBEM.__tests = {
            mockStore: {},
            origRequest: MBEM.blocks['mini-suggest'].prototype._request,
        };

        const { mockStore, origRequest } = window.MBEM.__tests;

        if (!MBEM.blocks['mini-suggest'].prototype._request.mocked) {
            MBEM.blocks['mini-suggest'].prototype._request = function(requestedVal: string, successCallback: Function) {
                var params = this._getMainUrlParams(requestedVal),
                    url = MBEM.appendUrlParams(this.params.url, params);

                if (requestedVal in mockStore) {
                    setTimeout(() => {
                        successCallback.call(this, requestedVal, mockStore[requestedVal], url, 0);

                        this.trigger('request', { text: requestedVal });
                    }, 0);
                } else {
                    origRequest.apply(this, arguments);
                }
            };
            MBEM.blocks['mini-suggest'].prototype._request.mocked = true;
        }

        mockStore[text] = mock;
    }, text, mock);
}
