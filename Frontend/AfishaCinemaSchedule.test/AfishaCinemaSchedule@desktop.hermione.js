'use strict';

const PO = require('./AfishaCinemaSchedule.page-object')('desktop');

specs({
    feature: '1орг. Витрина фильмов кинотеатра (десктоп)',
}, () => {
    const pathPref = '/$page/$parallel/$result/composite/tabs/about/afisha_cinema';
    const AFISHA_URL = 'https://afisha.yandex.ru';
    const shouldIncludeFromParam = query => {
        return query.from === 'afisha-venue-movie-desktop';
    };

    it('Проверка внешнего вида', async function() {
        await this.browser.yaOpenSerp({
            text: 'кинотеатр синема парк екатеринбург',
            data_filter: 'companies',
        }, PO.companiesList());

        await this.browser.assertView('plain', PO.oneOrg.afishaCinema());
    });

    it('Ссылки и счётчики', async function() {
        const afisha = PO.oneOrg.afishaCinema;

        await this.browser.yaOpenSerp({
            lr: '213',
            text: 'кинотеатр синема парк екатеринбург',
            data_filter: 'companies',
        }, afisha());

        // тайтл
        const url1 = await this.browser.yaParseHref(afisha.title.link());

        await this.browser.yaCheckURL(url1, {
            url: AFISHA_URL,
            queryValidator: shouldIncludeFromParam,
        }, 'Тайтл должен содержать ссылку на афишу и параметр from=afisha-venue-movie-desktop', { skipProtocol: true, skipPathname: true });

        await this.browser.yaCheckBaobabCounter(afisha.title.link(), {
            path: pathPref + '/title',
        });

        // тумба
        const url2 = await this.browser.yaParseHref(afisha.showcase.item.link());

        await this.browser.yaCheckURL(url2, {
            url: AFISHA_URL,
            queryValidator: shouldIncludeFromParam,
        }, 'Ссылка на тумбе должна содержать ссылку на афишу и параметр from=afisha-venue-movie-desktop', { skipProtocol: true, skipPathname: true });

        await this.browser.yaCheckBaobabCounter(afisha.showcase.item.link(), {
            path: pathPref + '/showcase/item/thumb',
        });

        // Все фильмы
        const url3 = await this.browser.yaParseHref(afisha.showcase.moreThumbLink());

        await this.browser.yaCheckURL(url3, {
            url: AFISHA_URL,
            queryValidator: shouldIncludeFromParam,
        }, 'Ссылка Все фильмы должна содержать ссылку на афишу и параметр from=afisha-venue-movie-desktop', { skipProtocol: true, skipPathname: true });

        await this.browser.yaShouldExist(afisha.showcase.moreThumbLink(), 'Нет кнопки "Посмотреть все фильмы"');

        await this.browser.execute(function(selector) {
            $(selector).scrollLeft(2000);
        }, afisha.showcase.scrollWrap());

        await this.browser.yaCheckBaobabCounter(afisha.showcase.moreThumbLink(), {
            path: pathPref + '/showcase/more',
        });
    });
});
