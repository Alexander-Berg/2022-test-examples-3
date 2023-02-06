describe('Поиск фильмов', function() {
    const entref = '0zH4sIAAAAAAAAAxNazZhTXKJXnJPrnJfu7eQRlJPqERic7J6TlhoelpGclV_p6-Ja7pNpAqQ9yz2dI3M9PfzSoozCSpNzQ82icnOKk4Idc70yHfOTDS3zosJNK6NCCipTIpIrfF0cjfxcAtPTnJ2qIoxScpLyvHL887wMU6pcK3yzIo09PcrT0fRU-oVEVmARN_ZzcTT0Dc62lWCSen_QHQB5i0F2tAAAAA';
    const text = 'фантастика%202010-2020';
    it('Открытие карусели', async function() {
        const offset = 0;

        const { browser } = this;

        await browser.yaOpenPage(`filmsSearch/${offset}/?entref=${entref}&text=${text}`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        // Проверка кнопки назад
        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('right');
        await browser.yaQuasarRCMove('back');
        await browser.yaWaitForNavigationState({ left: false });
        await browser.yaAssertQuasarState('plain');
    });
    it('Открытие третьего экрана карусели', async function() {
        const offset = 9;

        const { browser } = this;

        await browser.yaOpenPage(`filmsSearch/${offset}/?entref=${entref}&text=${text}`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Открытие карусели с ярким шильдиком 4к', async function() {
        const offset = 0;

        const { browser } = this;

        const entref = 'kin01355059%3Akin01355058%3Akin01227993%3Aruw8269586%3Aruw8262560%3Aenw64363917%3Akin01348105%3Aenum';
        const text = 'сериалы+в+4+к';

        await browser.yaOpenPage(`filmsSearch/${offset}/?entref=${entref}&text=${text}&video_codec=AVC%2CHEVC%2CVP9&video_format=SD%2CHD%2CUHD`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
});
