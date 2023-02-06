const querystring = require('querystring');

describe('Богатая карточка', function() {
    it('Экран фильма', async function() {
        const entref = '0oCgpydXc2OTI1MTUwEgtsc3QucmVjZmlsbRgCQgzRhNC40LvRjNC80Yu_OO7d';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/mainPage/?entref=${entref}`);

        await browser.yaSetTransitionDuration1ms();
        await browser.yaWaitForStopRendering();

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран сериала', async function() {
        const entref = '0oCgpydXc2MjM4OTQyEgdsc3RzcGFuGAIwj89m';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/mainPage/?entref=${entref}`);

        await browser.yaSetTransitionDuration1ms();
        await browser.yaWaitForStopRendering();

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран сериала с ярким шильдиком 4к', async function() {
        const entref = '0zH4sIAAAAAAAAA-PiKiottzAyszS1MBMKy87MMzA0NjU1MLW0QrAtIGwjI3NLS2MrhHoo08jUzMAqNa_czMTYzNjS0Byq08TC0MAUKF6aK8G0SebvKgB-SwBeagAAAA';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/mainPage/?entref=${entref}&video_codec=AVC%2CHEVC%2CVP9&video_format=SD%2CHD%2CUHD`);

        await browser.yaSetTransitionDuration1ms();
        await browser.yaWaitForStopRendering();

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран описания', async function() {
        const entref = '0oCglydXc2OTI4NjESEGxzdHByamFjcnV3MjkxOTkYAteJ_Io';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/descriptionPage/?entref=${entref}`);

        await browser.yaSetTransitionDuration1ms();
        await browser.yaWaitForStopRendering();

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран с похожими фильмами', async function() {
        const entref = '0oCglydXc2OTI4NjESEGxzdHByamFjcnV3MjkxOTkYAteJ_Io';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/related/?entref=${entref}`);

        await browser.yaSetTransitionDuration1ms();
        await browser.yaWaitForStopRendering();

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        // Проверка кнопки назад
        await browser.yaQuasarMove('right');
        await browser.yaQuasarRCMove('back');
        await browser.yaWaitForNavigationState({ left: false });
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран сезонов сериала', async function() {
        const entref = '0oCgpydXcxOTQzODMxGAJraOhY';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/seasons/?entref=${entref}`);

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран сериала (1 сезон, > 32 серий))', async function() {
        const entref = '0oCgpydXcxOTA5MTAyGAIegLTM';

        const { browser } = this;

        await browser.yaOpenPage(`videoSeasons/?clientY=66&entref=${entref}`);

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Экран сериала с невышедшими сериями', async function() {
        const entref = '0oCgpydXczNzkwMjk3GAI_-_9o';

        const { browser } = this;

        await browser.yaOpenPage(`videoSeasons/?season=3&clientY=2&entref=${entref}`);
        await browser.assertView('plain', 'body');

        // Невышедшие сериия не должны попадать в стейт
        await browser.yaAssertQuasarState('plain');
    });
    it('Корректно выдаёт саджесты переключения сезона для сериала с 2+ сезонами на первом сезоне', async function() {
        const { browser, PO } = this;

        // Детство Шелдона
        const entref = '0oCgpydXc3MDMyNDg3Eg1sc3QucmVjc2VyaWVzGAJCDtGB0LXRgNC40LDQu9GLZCBAfA';
        const currentSeason = 1;

        await browser.yaOpenPage('videoEntity/seasons/?' + querystring.stringify({
            quasarUI: 1,
            season: currentSeason,
            entref,
        }));

        await browser.assertView('suggests_2plus_s1', PO.NativeUI.Footer());
        await browser.yaAssertQuasarState('suggests_2plus_s1');
    });
    it('Корректно выдаёт саджесты переключения сезона для сериала с 2+ сезонами на последнем сезоне', async function() {
        const { browser, PO } = this;

        // Детство Шелдона
        const entref = '0oCgpydXc3MDMyNDg3Eg1sc3QucmVjc2VyaWVzGAJCDtGB0LXRgNC40LDQu9GLZCBAfA';
        const currentSeason = 2;

        await browser.yaOpenPage('videoEntity/seasons/?' + querystring.stringify({
            quasarUI: 1,
            season: currentSeason,
            entref,
        }));

        await browser.assertView('suggests_2plus_s2', PO.NativeUI.Footer());
        await browser.yaAssertQuasarState('suggests_2plus_s2');
    });
    it('Не выдаёт саджесты переключения сезона для сериала с 1 сезоном', async function() {
        const { browser, PO } = this;

        // Авеню 5
        const entref = '0oCgpydXc4MTE5OTgyEg1sc3QucmVjc2VyaWVzGAJCDtGB0LXRgNC40LDQu9GLr2p-kg';
        const currentSeason = 1;

        await browser.yaOpenPage('videoEntity/seasons/?' + querystring.stringify({
            quasarUI: 1,
            season: currentSeason,
            entref,
        }));

        await browser.assertView('suggests_1_s1', PO.NativeUI.Footer());
        await browser.yaAssertQuasarState('suggests_1_s1');
    });
    it('Экран сериала с новым бэкэндом', async function() {
        const entref = '0oCglydXc5NjExNzYYAuU8KZ4';
        // Из-за секции SEARCH_CGI без тестида не получится
        const expFlags = 'test-id=326098';

        const { browser } = this;

        await browser.yaOpenPage(`videoEntity/seasons/?entref=${entref}&${expFlags}`);
        await browser.assertView('plain', 'body');

        // Открывается без ошибок
        await browser.yaAssertQuasarState('plain');
    });
});
