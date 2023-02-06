/** Сделать оверлей модалки непрозрачным черным */
export async function yaMakeBlackOverlay(this: WebdriverIO.Browser) {
    await this.execute(() => {
        // @ts-ignore
        document.querySelector('.Modal-Overlay, .Drawer-Overlay').style.background = 'black';
    });
}
