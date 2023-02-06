interface LoginCredentials {
    login: string;
    password: string;
}

interface TUSParams {
    login?: string | Array<string>;
    tags?: string | Array<string>;
    tus_consumer?: string;
    lock_duration?: number;
    ignore_locks?: boolean;
}

interface LoginParams {
    [key: string]: string | LoginCredentials;
}

interface Auth {
    tus(params: TUSParams): void;
    login(params: LoginParams): void;
    createAndLogin(params: TUSParams): void;
}

type BrowserName = 'chrome-desktop' | 'chrome-phone' | '';

interface HermioneSkip {
    in(browser: BrowserName, description: string): HermioneSkip;
    notIn(browser: BrowserName, description: string): HermioneSkip;
}

interface HermioneOnly {
    in(browser: BrowserName): HermioneOnly;
    notIn(browser: BrowserName): HermioneOnly;
}

interface Hermione {
    auth: Auth;
    only: HermioneOnly;
    skip: HermioneSkip;
}

interface HermioneAssertViewOptions {
    ignoreElements: string | string[];
    tolerance: number;
    antialiasingTolerance: number;
    allowViewportOverflow: boolean;
    captureElementFromTop: boolean;
    compositeImage: boolean;
    screenshotDelay: number;
}

interface Browser {
    assertView(
        name: string,
        selector: string | string[],
        options?: HermioneAssertViewOptions
    ): Promise<void>;
    url(url?: string): Promise<{ value?: string } | undefined>;
    isExisting(selector: string): Promise<boolean>;
    click(selector: string): Promise<void>;
    waitForVisible(selector: string): Promise<void>;
    refresh(): Promise<void>;
    pause(ms: number): Promise<void>;
    scroll(selectorOrXOffset: string | number, yOffset?: number): Promise<void>;
    waitUntil(
        condition: (...args: any[]) => boolean | Promise<boolean>,
        timeout = 500,
        timeoutMsg?: string,
        interval = 500
    ): Promise<void>;
    yaAssertView(
        name: string,
        selector: string | string[],
        options?: HermioneAssertViewOptions
    ): Promise<void>;
    yaAssertInViewport(selector: string): Promise<void>;
    yaLoginFast(login: string, password: string): Promise<void>;
    yaIsMobile(): Promise<boolean>;
    yaScroll(selectorOrOffset: string | number): Promise<void>;
    yaWaitForHidden(
        selector: string,
        message?: string,
        timeout = 500
    ): Promise<void>;
    yaWaitForVisible(
        selector: string,
        message?: string,
        timeout = 500,
        reverse = false
    ): Promise<void>;
}

interface HermioneData {
    authUser: LoginCredentials;
}

namespace Mocha {
    class HermioneTest extends Mocha.Test {
        hermioneCtx: HermioneData;
    }
    class HermioneContext extends Mocha.Context {
        browser: Browser;
        currentTest?: HermioneTest;
    }
    type HermioneAsyncFunc = (this: HermioneContext) => PromiseLike<any>;
    interface TestFunction {
        (title: string, fn?: HermioneAsyncFunc): Mocha.Test;
    }
}

declare const hermione: Hermione;
