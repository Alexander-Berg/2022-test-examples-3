export class Gonec {
    private widget: HTMLElement;
    private currentIframe: HTMLIFrameElement | null;

    private iframeSpy;

    private get iframe() {
        const iframe = this.currentIframe || (this.currentIframe = this.widget.querySelector<HTMLIFrameElement>('iframe'));

        if (!this.iframeSpy && iframe && iframe.contentWindow) {
            this.iframeSpy = jest.spyOn(iframe.contentWindow, 'postMessage');
        }

        return iframe;
    }

    public get spy() {
        if (!this.iframeSpy) {
            this.iframe;
        }

        return this.iframeSpy;
    }
    public setWidget(widget: HTMLElement) {
        this.widget = widget;
        this.currentIframe = null;
        this.iframeSpy = undefined;
    }

    public waitForPost() {
        return new Promise((resolve) => {
            setTimeout(resolve, 0);
        });
    }

    public sendFromIframe<D>(data: D, target = '*') {
        if (this.iframe && this.iframe.contentWindow) {
            this.iframe.contentWindow.parent.postMessage(data, target);
        }

        return this.waitForPost();
    }

    public sendFromWindow<D>(data: D, target = '*') {
        window.postMessage(data, target);

        return this.waitForPost();
    }

    public sendToIframe<D>(data: D, target = '*') {
        if (this.iframe && this.iframe.contentWindow) {
            this.iframe.contentWindow.postMessage(data, target);
        }

        return this.waitForPost();
    }
}
