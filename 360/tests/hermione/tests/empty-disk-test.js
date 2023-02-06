const { PUBLIC_AUDIO_FILE_URL } = require('../config/index').consts;

describe('Пользователь без диска ->', () => {
    hermione.auth.createAndLogin({ tus_consumer: 'disk-front-client' });
    it('diskpublic-561: diskpublic-2276: Сохранение файла на Диск пользователем без диска', async function() {
        const bro = this.browser;
        await bro.url(PUBLIC_AUDIO_FILE_URL);

        await bro.yaSaveToDisk();
    });
});
