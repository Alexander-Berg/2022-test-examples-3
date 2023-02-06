interface IOptions {
    /** Действие, которое необходимо выполнить для открытия новой вкладки. */
    action: (this: WebdriverIO.Browser) => void;
    /** Действие, которое будет выполенно на новой вкладке. */
    checkTab?: (this: WebdriverIO.Browser) => void;
    /** Сообщение, если вкладка не открылась. */
    message?: string;
    timeout?: number;
}

/**
 * Проверяет, была ли открыта новая вкладка и возвращает её id.
 */
export async function yaCheckNewTabOpen(
    this: WebdriverIO.Browser,
    options: IOptions,
) {
    const message = options.message ?? '';
    const timeout = options.timeout ?? 5000;

    const [oldTabIds, startTabId] = await Promise.all([this.getTabIds(), this.getCurrentTabId()]);
    let newTabId: string = '';

    const getNewTabId = (nextTabIds: string[]): string | undefined => {
        if (nextTabIds.length !== oldTabIds.length) {
            return nextTabIds.find(tabId => !oldTabIds.includes(tabId));
        }
        return;
    };
    const isOpenTab = async(): Promise<boolean> => {
        const nextTabIds = await this.getTabIds();
        const tabId = getNewTabId(nextTabIds);

        if (tabId) {
            newTabId = tabId;
            return true;
        }
        return false;
    };

    await options.action.call(this);
    await this.yaWaitUntil(`[Новая вкладка не открылась] ${message}`, isOpenTab, timeout, 500);

    if (options.checkTab) {
        await this.switchTab(newTabId);
        await this.yaWaitForVisible('body > *', `[Страница на новой вкладке не загрузилась] ${message}`);
        await options.checkTab.call(this);
        await this.switchTab(startTabId);
        await this.execute(function() {
            document.dispatchEvent(new Event('visibilitychange'));
        });
    }

    return newTabId;
}
