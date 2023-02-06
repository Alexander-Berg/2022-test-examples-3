/**
 * Дожидается загрузки страницы SKU.
 */
export async function yaWaitForSKUPage(
    this: WebdriverIO.Browser,
) {
    await this.yaWaitForVisible('.Card', 3000);
    await this.yaWaitUntil('Не загрузилась страница SKU', () => {
        return this.execute(() => {
            // @ts-expect-error в тайпингах Ya нет стора.
            return window.Ya.store.getState().pages.sku.requestStatus === 'success';
        });
    }, 5000);
}
