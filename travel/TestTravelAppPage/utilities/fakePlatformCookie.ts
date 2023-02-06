import Cookie from 'js-cookie';

import {TPlatform} from 'constants/platforms/TPlatforms';
import {WEB, IOS, ANDROID} from 'server/constants/platforms';

const FAKE_IOS_COOKIE_NAME = 'fake-ios';
const FAKE_ANDROID_COOKIE_NAME = 'fake-android';

export function readFakePlatformCookie(): TPlatform {
    if (Cookie.get(FAKE_IOS_COOKIE_NAME)) {
        return IOS;
    }

    if (Cookie.get(FAKE_ANDROID_COOKIE_NAME)) {
        return ANDROID;
    }

    return WEB;
}

export function setFakePlatformCookie(value: string): void {
    switch (value) {
        case WEB: {
            Cookie.remove(FAKE_IOS_COOKIE_NAME);
            Cookie.remove(FAKE_ANDROID_COOKIE_NAME);

            return;
        }
        case IOS: {
            Cookie.remove(FAKE_ANDROID_COOKIE_NAME);
            Cookie.set(FAKE_IOS_COOKIE_NAME, '1');

            return;
        }
        case ANDROID: {
            Cookie.remove(FAKE_IOS_COOKIE_NAME);
            Cookie.set(FAKE_ANDROID_COOKIE_NAME, '1');

            return;
        }
    }
}
