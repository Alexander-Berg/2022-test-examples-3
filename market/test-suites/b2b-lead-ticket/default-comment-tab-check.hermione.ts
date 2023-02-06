import 'hermione';
import {login, PageObject} from '../../helpers';

/** ссылка на любое обращение b2b Лид очереди */
const PAGE_URL = '/entity/ticket@176424884';

describe(`ocrm-1527: В обращениях B2B лидов по умолчанию открыта внутренняя заметка`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`Открывается страница тикета в нужной очереди и проверяется активность необходимого таба`, async function() {
        const neededTab = new PageObject(
            this.browser,
            'body',
            '[data-ow-test-comment-type-button="comment$internal-active"]'
        );

        await neededTab.isDisplayed('таб "Внутренняя заметка" не активен');
    });
});
