import 'hermione';
import {execScript, extractGid, login, PageObject} from '../../helpers';
import {CASES} from './default-tab-config';

const getNeededTabRoot = neededTabRoot => `[${neededTabRoot}][data-tab-active=true]`;

/**
 * План теста:
 * 1) Получаем gid тикета со нужными параметрами
 * 2) Открываем тикет
 * 3) Проверяем что тикет откался на нужном табе
 */

CASES.forEach(testCase => {
    describe(testCase.name, () => {
        beforeEach(function() {
            return login('', this);
        });

        it(testCase.expectation, async function() {
            const ticketRaw = await execScript(this.browser, testCase.script);
            const ticket = extractGid(ticketRaw);
            const ticketUrl = `entity/${ticket}`;
            const neededTab = new PageObject(this.browser, 'body', getNeededTabRoot(testCase.neededTab));

            await this.browser.url(ticketUrl);
            await neededTab.isDisplayed('В обращении не активен нужный таб');
        });
    });
});
