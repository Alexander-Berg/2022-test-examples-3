const { consts, folder, publicReadOnly } = require('../config');

describe('Заглушка для старых браузеров ->', () => {
    it('diskpublic-1716: Паблик файла. Заглушки в старом браузере', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_AUDIO_FILE_URL);

        await bro.pause(500); // должен пропасть скролл
        await bro.assertView('diskpublic-1716', 'body');
    });

    it('diskpublic-1717: Паблик папки.Заглушки в старом браузере', async function() {
        const bro = this.browser;
        await bro.url(folder.PUBLIC_FOLDER_URL);

        await bro.pause(500); // должен пропасть скролл
        await bro.assertView('diskpublic-1717', 'body');
    });

    it('diskpublic-2712: Паблик залимитированного файла: Просмотр в старом браузере ', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_EXE_ANTIFO_FILE_URL);

        await bro.pause(500); // должен пропасть скролл
        await bro.assertView('diskpublic-2712', 'body');
    });

    it('diskpublic-3022: Паблик с запретом скачивания в старых браузерах', async function() {
        const bro = this.browser;
        await bro.url(publicReadOnly.document.url);

        await bro.pause(500); // должен пропасть скролл
        await bro.assertView('diskpublic-3022', 'body');
    });
});
