import puppeteer from 'puppeteer';
import { Trace, TraceEvent } from '../../../trace-processor';
import { Argv } from '../..';

export async function test(argv: Argv): Promise<void> {
    const browser = await puppeteer.launch(argv.config.puppeteerOptions);
    const page = await browser.newPage();
    await page.tracing.start({});
    await page.goto('about:blank');
    const trace = await page.tracing.stop();
    await browser.close();

    const parsedTrace: Trace = JSON.parse(trace.toString());
    const hasTicountSupport = parsedTrace.traceEvents.some((event: TraceEvent) => event.ticount);

    if (!hasTicountSupport) {
        throw new Error('ticount/tidelta is not supported on this host');
    }

    console.log('ticount/tidelta is supported on this host');
}
