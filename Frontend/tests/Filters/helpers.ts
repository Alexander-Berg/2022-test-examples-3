export async function checkFilter(
    browser: WebdriverIO.Browser,
    baobabPath: string,
    action = async() => {},
) {
    // Находим ноду кнопки и запоминаем reqid до клика,
    // т.к, при проверке счетчика выдача успевает перезагрузиться reqid меняется и счетчик не находится в логах
    const productCardSelector = '.ProductCardsList-Wrapper .ProductCard';
    const clearButtonNode = await browser.yaGetBaobabNode({ path: baobabPath, source: 'redir' });
    const firstReqid = await browser.yaGetReqId();

    await action.call(null);
    await browser.yaWaitElementsChanging(productCardSelector, {
        timeoutMsg: 'Выдача не обновилась',
    });
    // Ждем когда залогируется событие клика
    await browser.waitUntil(async() => {
        const clickEvents = await browser.yaGetBaobabSentEvents('click', firstReqid);
        const submitButtonEvent = clearButtonNode && clickEvents && clickEvents.find(
            event => event.id === clearButtonNode.id,
        );

        return Boolean(submitButtonEvent);
    }, { timeout: 5000 });
}

export async function prepareModalAssertView(browser: WebdriverIO.Browser) {
    await browser.execute(() => {
        // @ts-ignore
        document.querySelector('.SearchPage').style.opacity = 0;
    });
}
