import {assert} from 'chai';
import urljoin from 'url-join';

const currentHost = process.env.E2E_URL || '';

describe('Предобработка URL запроса', function () {
    it('Если URL не содержит экранированных символов, редирект не должен происходить', async function () {
        const goodUrl = new URL(
            urljoin(
                currentHost,
                '/avia/?adult_seats=1&children_seats=0&fromId=c213&infant_seats=0&klass=economy&oneway=2&return_date=2021-09-29&toId=c239&when=2021-09-25',
            ),
        );

        await this.browser.url(goodUrl.href);

        const actualUrl = new URL(await this.browser.getUrl());

        assert.equal(
            actualUrl.search,
            goodUrl.search,
            'Браузер должен остаться на той же странице',
        );

        assert.equal(
            actualUrl.pathname,
            goodUrl.pathname,
            'Браузер не должен изменить pathname',
        );
    });

    it('Если URL содержит экранированные символы, должен произойти редирект на правильный URL', async function () {
        const initialUrl = new URL(
            urljoin(currentHost, '/avia/?adult_seats=1&amp;children_seats=0'),
        );
        const expectedUrl = new URL(
            urljoin(currentHost, '/avia/?adult_seats=1&children_seats=0'),
        );

        await this.browser.url(initialUrl.href);

        const actualUrl = new URL(await this.browser.getUrl());

        assert.include(
            actualUrl.search,
            expectedUrl.search,
            'Браузер должен сделать правильный редирект',
        );

        assert.equal(
            actualUrl.pathname,
            expectedUrl.pathname,
            'Браузер не должен изменить pathname',
        );
    });

    it('Если URL закодирован правильно, не должен образоваться бесконечный редирект', async function () {
        const goodUrl = new URL(
            urljoin(currentHost, '/avia/?adult_seats=1&children_seats=0'),
        );

        await this.browser.url(goodUrl.href);

        const actualUrl = new URL(await this.browser.getUrl());

        assert.include(
            actualUrl.search,
            goodUrl.search,
            'Браузер должен остаться на той же странице',
        );

        assert.equal(
            actualUrl.pathname,
            goodUrl.pathname,
            'Браузер не должен изменить pathname',
        );
    });
});
