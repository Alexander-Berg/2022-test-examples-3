'use strict';

const PO = require('./OrgAfishaCinema.page-object')('touch-phone');

specs({
    feature: 'Одна организация',
    type: 'Афиша кино',
}, () => {
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кинотеатр синема парк екатеринбург',
            data_filter: 'companies',
            exp_flags: 'GEO_1org_afisha_cinema_react=1',
        }, PO.oneOrg.orgAfishaCinemaList());

        await browser.assertView('plain', PO.oneOrg.orgAfishaCinemaList());
    });

    it('Ссылки и счетчики', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кинотеатр синема парк екатеринбург',
            data_filter: 'companies',
            exp_flags: 'GEO_1org_afisha_cinema_react=1',
        }, PO.oneOrg.orgAfishaCinemaList());

        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.orgAfishaCinemaList.title(),
            url: {
                href: 'https://afisha.yandex.ru/yekaterinburg/cinema/places/sinema-park-starlight-na-urale?place-schedule-preset=today',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/composite/afisha_cinema/title',
            },
        });

        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.orgAfishaCinemaList.firstItem.link(),
            url: {
                href: 'https://afisha.yandex.ru/yekaterinburg/cinema/zemlianichnaia-poliana?from=afisha-venue-movie-desktop&schedule-date=2022-05-23',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/composite/afisha_cinema/scroller/companies-discovery-movie/companies-discovery-card/item',
            },
        });

        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.orgAfishaCinemaList.more(),
            url: {
                href: 'https://afisha.yandex.ru/yekaterinburg/cinema/places/sinema-park-starlight-na-urale?place-schedule-preset=today',
                ignore: ['pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/composite/afisha_cinema/scroller/more',
            },
        });
    });
});
