import { Gonec } from './gonec';
import { ChatWidget as Widget } from '../widget';

export class Helper {
    private widget: Widget;

    constructor(private gonec: Gonec) {}

    public setWidget(widget: Widget) {
        this.widget = widget;
    }

    public showChat(params?: ChatWidget.ShowChatParams, sendReady = true) {
        this.widget.showChat(params);

        if (!sendReady) {
            return;
        }

        return this.ready();
    }

    public ready() {
        return this.gonec.sendFromIframe({
            namespace: 'messenger',
            type: 'ready',
        });
    }

    public async setFullscreen(showChat = false) {
        if (showChat) {
            await this.showChat();
        }

        return this.gonec.sendFromIframe({
            namespace: 'messenger',
            type: 'fullscreenOn',
        });
    }

    public async cancelFullscreen(showChat = false) {
        if (showChat) {
            await this.showChat();
        }

        return this.gonec.sendFromIframe({
            namespace: 'messenger',
            type: 'fullscreenOff',
        });
    }
}
