export function waitUntilScroll(
    this: WebdriverIO.Browser,
    condition: (scrollLeft: number) => boolean,
    timeoutMsg: string,
) {
    return this.waitUntil(async() => {
        const scrollLeft = await this.execute(function() {
            const scroller = document.querySelector('.Scroller-ItemsScroller');
            return scroller ? scroller.scrollLeft : -1;
        });
        return condition(scrollLeft);
    }, {
        timeout: 1000,
        interval: 300,
        timeoutMsg,
    });
}
