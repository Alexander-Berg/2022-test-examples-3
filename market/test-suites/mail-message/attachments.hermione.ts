import 'hermione';
import url from 'url';
import {expect} from 'chai';

import {login} from '../../helpers';
import {AttachmentsTable} from '../../../src/components/jmf/EmployeeMailMessage/components/AttachmentsTable/__pageObject__';
import {AttachmentsLightbox} from '../../../src/controls/AttachmentsLightbox/__pageObject__';
import {getFilePathFromUrl, clearDefaultDirectory, waitForFileExists} from '../../helpers/file';

const PAGE_URL = '/entity/employeeMailMessage@178083184';

/**
 * Проверяем, что:
 * Таблица вложений отображается на странцие личного письма;
 * Картинки открываются по клику на ссылку с названием,
 * Вложения остальных типов по клику скачиваются.
 */
describe('ocrm-1498: Просмотр вложений в личных письмах', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    afterEach(clearDefaultDirectory);

    it(`Клик по названию должен открывать картинку-вложение для просмотра`, async function() {
        const attachmentsTable = new AttachmentsTable(this.browser);
        const attachmentsLightbox = new AttachmentsLightbox(this.browser);
        const isDisplayed = await attachmentsTable.isDisplayed();

        expect(isDisplayed, 'Таблицы вложений нет').to.equal(true);

        await attachmentsTable.selectNameLinkItem(0);

        const isImageOpened = await attachmentsLightbox.isExisting();

        expect(isImageOpened, 'Картинка не открылась').to.equal(true);
    });

    it(`Клик по названию должен скачивать вложение`, async function() {
        hermione.skip.in(['yandex-browser', 'chrome'], 'Тикет на починку: OCRM-9783');

        const attachmentsTable = new AttachmentsTable(this.browser);

        const isDisplayed = await attachmentsTable.isDisplayed();

        expect(isDisplayed, 'Таблицы вложений нет').to.equal(true);

        await attachmentsTable.selectNameLinkItem(1);

        const urlString = await attachmentsTable.selectNameLinkHref(1);
        const {baseUrl = ''} = this.browser.options;
        const downloadUrl = new URL(url.resolve(baseUrl, urlString));

        const isFileExist = await waitForFileExists(getFilePathFromUrl(downloadUrl));

        expect(isFileExist, 'Файл не скачался').to.equal(true);
    });
});
