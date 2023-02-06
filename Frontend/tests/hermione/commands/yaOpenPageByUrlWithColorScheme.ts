// Обрабатывает настройку prefersColorScheme перед открытием страницы
export async function yaOpenPageByUrlWithColorScheme(
    this: WebdriverIO.Browser,
    url: string,
    colorScheme?: string,
) {
    const meta = await this.getMeta();
    const prefersColorScheme = meta.prefersColorScheme as string;
    const scheme = colorScheme || prefersColorScheme;

    if (['dark', 'light'].includes(scheme)) {
        const puppeteer = await this.getPuppeteer();
        const [page] = await puppeteer.pages();
        await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: scheme }]);
    }

    await this.yaOpenPageByUrl(url);
}
