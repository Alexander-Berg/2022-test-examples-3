import Cookie from 'js-cookie';

const APPLICATION_COOKIE_NAME = 'travel-app';

export function readApplicationCookie(): string {
    return String(Cookie.get(APPLICATION_COOKIE_NAME) === 'true');
}

export function setApplicationCookie(value: string): void {
    return Cookie.set(APPLICATION_COOKIE_NAME, value);
}

export function removeApplicationCookie(): void {
    Cookie.remove(APPLICATION_COOKIE_NAME);
}
