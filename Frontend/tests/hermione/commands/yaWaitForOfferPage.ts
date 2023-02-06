/**
 * Дожидается загрузки страницы оффера.
 */
export async function yaWaitForOfferPage(
    this: WebdriverIO.Browser,
) {
    await this.yaWaitForVisible('.Card', 3000);
    await this.yaWaitUntil('Не загрузилась страница оффера', () => {
        return this.execute(() => {
            // @ts-expect-error в тайпингах Ya нет стора.
            return window.Ya.store.getState().pages.offer.requestStatus === 'success';
        });
    }, 5000);
}
