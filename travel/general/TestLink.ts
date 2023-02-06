import {MINUTE} from 'helpers/constants/dates';

import getRelativePathName from 'helpers/utilities/getRelativePathName';

import {Component} from './Component';

export class TestLink extends Component {
    async getRelativePathName(): Promise<string | null> {
        const href = await this.getHref();

        if (!href) {
            return null;
        }

        return getRelativePathName(href, await this.browser.getUrl());
    }

    async getUrl(): Promise<URL | null> {
        const href = await this.getHref();

        if (!href) {
            return null;
        }

        return new URL(href);
    }

    /**
     * Проверяет урл на статус через XHR
     */
    async getRequestStatus(): Promise<number> {
        const href = await this.getHref();

        if (!href) {
            return 0;
        }

        await this.browser.setTimeout({script: MINUTE});

        return this.browser.execute(url => {
            const xhr = new XMLHttpRequest();

            xhr.open('GET', url, false);

            xhr.send();

            return xhr.status;
        }, href);
    }

    private async getHref(): Promise<string | null> {
        return this.getAttribute('href');
    }
}
