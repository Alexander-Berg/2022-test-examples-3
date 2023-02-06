import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import Button from '../../page-objects/button';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {CLEAR_ALL_SEQUENCE} from '../../constants';
import {
    PROPERTIES_LIST_ACTION_WRAPPER_LABEL,
    PROPERTIES_LIST_ATTRIBUTE_WRAPPER_LABEL,
} from '../../../src/components/jmf/PropertiesList/constants';

const PAGE_URL = '/entity/ticket@139095218';
const TWO_MINUTES = 120000;
/**
 * Делим тут на 2 минуты допуская, что сьюты вряд ли будут выполняться чаще чем раз в 2 минуты.
 */
const EMAIL_PREFIX = Math.floor(Date.now() / TWO_MINUTES);

interface EmailValue {
    email: string;
    ellipsed: boolean;
    title: string;
}

const VALUES: EmailValue[] = [
    {
        email: `${EMAIL_PREFIX}@yandex.ru`,
        ellipsed: false,
        title: 'Короткий адрес должен отображаться целиком',
    },
    {
        email: `long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long-long@yandex.ru`,
        ellipsed: true,
        title: 'Длинный адрес должен отображаться сокращенным',
    },
];

declare const hermione: Hermione.GlobalHelper;

/**
 * План теста:
 * 1. Найти кнопку "карандаш" (редактирование)
 * 2. Кликнуть по кнопке
 * 3. Найти тело модалки и в нем нужное поле
 * 4. Очистить поле
 * 5. Заполнить поле значением мейла
 * 6. Найти блок контролов модалки и в нем кнопу "Сохранить"
 * 7. Убедиться что кнопка не заблочена
 * 8. Нажать кнопку, кнопка заблокируется.
 * 9. Дождаться разблокировки кнопки.
 * 10. Найти блок атрибутов карточки и в нем нужное поле
 * 11. Считать атрибуты поля и текущее значение
 * 12. По атрибутам offsetWidth и scrollWidth вычислить, применился ли на поле ellipsis
 * 13. Сверить Значение поля с тем, что сохраняли в п. 5, значения должны совпасть.
 * 14. Сверить значение вычисленное в п. 12 с заданным, они должны совпасть.
 */
describe(`ocrm-1118: Проверка обрезки слишком длинных значений атрибутов типа EMail`, () => {
    /**
     * Переписан на тириуме с использованием скриншотов т.к. на JS работает нестабильно https://st.yandex-team.ru/OCRM-8620
     */
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    VALUES.forEach(({email, ellipsed, title}) => {
        hermione.skip.in(['yandex-browser', 'chrome'], 'Переписан на скриншотный тириум тест OCRM-8620');
        it(title, async function() {
            const editButton = new Button(
                this.browser,
                'body',
                `[data-ow-test-${PROPERTIES_LIST_ACTION_WRAPPER_LABEL}="edit"]`
            );
            const editClientEmail = new ContentWithLabel(
                this.browser,
                '[data-ow-test-modal-body]',
                `[data-ow-test-${PROPERTIES_LIST_ATTRIBUTE_WRAPPER_LABEL}="clientEmail"]`
            );
            const saveButton = new Button(
                this.browser,
                '[data-ow-test-modal-controls]',
                `[data-ow-test-${PROPERTIES_LIST_ACTION_WRAPPER_LABEL}="save"]`
            );
            const clientEmail = new ContentWithLabel(
                this.browser,
                '[data-ow-test-content="properties-cardTop"]',
                '[data-ow-test-attribute-container="clientEmail"]'
            );

            await editButton.isDisplayed();
            await (await editButton.button).click();
            await editClientEmail.isDisplayed();
            await editClientEmail.setValue(CLEAR_ALL_SEQUENCE);
            await editClientEmail.setValue(email);
            await saveButton.isDisplayed();
            /**
             * Если значения полей не изменились (например мы записали то же самое значение в поле, что было до того),
             * то кнопка сохранения будет заблочена. Тут мы это дело проверим и упадем, если да.
             */
            const saveButtonEnabled = await (await saveButton.button).isEnabled();

            expect(saveButtonEnabled).to.equal(
                true,
                'Упс, кнопка "сохранить" задизейблена. Значит мы пытаемся вписать в поле то же самое значение. Попробуйте повторить тест через пару минут.'
            );

            await (await saveButton.button).click();
            await saveButton.waitForInvisible();
            await clientEmail.isDisplayed();

            const emailOffsetWidth = await (await clientEmail.buttonContents).getProperty('offsetWidth');
            const emailScrollWidth = await (await clientEmail.buttonContents).getProperty('scrollWidth');

            const emailValue = (await (await clientEmail.buttonContents).getText()).trim();
            const isEllipsed = Number(emailOffsetWidth) < Number(emailScrollWidth);

            expect(emailValue).to.equal(email, 'Значения заданного мейла и фактического не совпали.');
            expect(isEllipsed).to.equal(ellipsed, 'Стиль элемента не соответствует ожидаемому значению.');
        });
    });
});
