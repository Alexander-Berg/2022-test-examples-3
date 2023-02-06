import 'hermione';
import {login, PageObject} from '../../helpers';

/** ссылка на любое обращение соц.сетей */
const PAGE_URL = '/entity/ticket@180167939';

describe(`ocrm-1467: Обращения соц. сетей открываются с активным табом "Сообщение в социальную сеть"`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`Открывается страница обращения нужного типа и проверяется активность необходимого таба`, async function() {
        const neededTab = new PageObject(
            this.browser,
            'body',
            '[data-ow-test-comment-type-button="comment$socialMessagingOut-active"]'
        );

        await neededTab.isDisplayed('таб "Сообщение в социальную сеть" не активен');
    });
});
