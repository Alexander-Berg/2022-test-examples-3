import { ChannelTransport, IframePostMessageTransport } from '@yandex-int/messenger.channels';

import { Widget } from '../Widget';
import { Plugin, Options, BeforeShowEvent, UIPlugin, GetChildWindowTransportParams } from '../types';

export interface MockedHooksFunctions {
    initCallback: (options: Options, widget: Widget) => void;
    LCBeforeShowCallback?: (event: BeforeShowEvent) => void;
    LCReadyCallback?: () => void;
    LCBeforeHideCallback?: () => void;
    LCHiddenCallback?: () => void;
    LCCloseCallback?: () => void;
    LCShownCallback?: () => void;
    LCBeforeRequestCallback?: () => void;
    LCErrorCriticalCallback?: () => void;
}

export class MockedPlugin implements Plugin {
    constructor(
        private mockedFunctions: MockedHooksFunctions,
    ) {}

    public init(params: Options, widget: Widget) {
        this.mockedFunctions.initCallback(params, widget);
    }

    public LCBeforeShow(event: BeforeShowEvent) {
        this.mockedFunctions?.LCBeforeShowCallback(event);
    }

    public LCReady() {
        this.mockedFunctions?.LCReadyCallback();
    }

    public LCHidden() {
        this.mockedFunctions?.LCHiddenCallback();
    }

    public LCBeforeHide() {
        this.mockedFunctions?.LCBeforeHideCallback();
    }

    public LCClose() {
        this.mockedFunctions?.LCCloseCallback();
    }

    public LCShown() {
        this.mockedFunctions?.LCShownCallback();
    }

    public LCBeforeRequest() {
        this.mockedFunctions?.LCBeforeRequestCallback();
    }

    public LCErrorCritical() {
        this.mockedFunctions?.LCErrorCriticalCallback();
    }
}

export class MockedUIPlugin implements UIPlugin {
    constructor(
        private childWindow: Window,
        private origin: string,
    ) {}

    public init() {}

    public mount() {}

    public getChildWindowTransport(
        params: GetChildWindowTransportParams,
        callback: (transport: ChannelTransport) => void,
    ) {
        const iframeTransport = new IframePostMessageTransport(
            this.childWindow,
            this.origin,
        );

        callback(iframeTransport);
    }
}
