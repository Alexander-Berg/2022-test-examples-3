interface IOptions {
    /**
     * Дождаться загрузки следующей страницы.
     * @default false
     */
    waitMore?: boolean;
}

/**
 * Дожидается загрузки страницы поисковой выдачи.
 */
export async function yaWaitForSearchPage(
    this: WebdriverIO.Browser,
    options: IOptions = {},
) {
    const waitMore = options.waitMore ?? false;

    await this.yaWaitForVisible('.SearchPage', 3000);
    await this.yaWaitUntil('Не загрузилась страница выдачи', () => {
        return this.execute(options => {
            // @ts-expect-error в тайпингах Ya нет стора.
            const { requestStatus, loadMoreRequestStatus } = window.Ya.store.getState().pages.search;
            const requestSuccess = requestStatus === 'success';

            if (options.waitMore) {
                return requestSuccess && loadMoreRequestStatus === 'success';
            }

            return requestSuccess;
        }, { waitMore });
    }, 5000);
}
