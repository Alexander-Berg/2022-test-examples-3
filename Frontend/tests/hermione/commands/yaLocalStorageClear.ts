/**
 * Очистить LocalStorage
 */
export function yaLocalStorageClear(
    this: WebdriverIO.Browser,
): Promise<void> {
    return this.execute(function() {
        try {
            localStorage.clear();
        } catch (e) { }
    });
}
