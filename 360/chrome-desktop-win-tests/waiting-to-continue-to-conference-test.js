const { assertView, openConnectionScreenWin } = require('../helpers/joinToConferenceHelper');
const clientObjects = require('../page-objects/client');

describe('Экран подключения ->', () => {
    it('telemost-279: Экран "Вы подключаетесь к видеовстрече"', async function () {
        const bro = this.browser;
        await openConnectionScreenWin(bro, { anonymous: true });
        await assertView(bro, 'telemost-279-waiting-to-continue-to-conference-screen', [clientObjects.common.tooltip()]);
    });
});
