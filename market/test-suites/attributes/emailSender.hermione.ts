import 'hermione';
import {expect} from 'chai';
import {alertIsPresent} from 'wdio-wait-for';

import {login} from '../../helpers';
import Button from '../../page-objects/button';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {SPACES_REGEXP, TIMEOUT_MS} from '../../constants';

const PAGE_URL_ROOT = '/entity/brand@114825884';
const PAGE_URL_EDIT = `${PAGE_URL_ROOT}/edit`;

interface EmailValue {
    email: string;
    valid: boolean;
}

const VALUES: EmailValue[] = [
    {
        email: 'testsuperstar@yandex.ru',
        valid: true,
    },
    {
        email: 'testsuperstaryandex.ru',
        valid: false,
    },
    {
        email: '"Ninja Алексей" <testsuperstar@yandex.ru>',
        valid: true,
    },
    {
        email: 'testsuperstar@ya',
        valid: false,
    },
];

/**
 * План теста:
 * 1. Найти поле атрибута типа EMail
 * 2. Убедиться, что поле видимо
 * 3. Очистить поле
 * 4. Вставить в поле заготовленный вариант мейла
 * 5. Считать значения CSS-классов враппера
 * 6. Искать в списке классов .Mui-error
 * 7. Вычислить валидность по наличию калсса из п. 6
 * 8. Значение валидности из п. 7 должно совпадать с заданным
 * 9. Повторить с п. 1 для всех заготовленных значений.
 * 10. Найти кнопку "Отменить"
 * 11. Кликнуть по кнопке
 * 12. Ответить утвердительно на запрос алерта
 * 13. После небольшой паузы считать значение текущего урла
 * 14. Урл должен совпадать с заданным
 *
 * п.п. 10-14 необходимы, т.к. при уходе с формы с несохраненными изменениями приложение
 * выкидвает алерт, и его не удается выловить другими способами, а если его не выловить, то тесты падают.
 */
describe(`ocrm-1502: Валидация разных вариантов значений EMail адресов`, () => {
    beforeEach(function() {
        return login(PAGE_URL_EDIT, this);
    });

    it('должна работать корректно', async function() {
        const emailSender = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-attribute-container="emailSender"]'
        );

        const cancelButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="cancel-отменить"]'
        );

        await emailSender.isDisplayed();
        const emailSenderWrapper = await emailSender.wrapper;

        // Вводим и проверяем email'ы один за другим, формируя массив для проверки
        let resultCheck: Promise<EmailValue[]> = Promise.resolve([]);

        VALUES.forEach(({email}) => {
            resultCheck = resultCheck.then(
                async (values = []): Promise<EmailValue[]> => {
                    // клик по кнопке очистки поля
                    await emailSender.clickButton();
                    await emailSender.setValue(email);

                    const classes = await emailSenderWrapper.getAttribute('class');
                    const isValid = !classes.split(SPACES_REGEXP).includes('Mui-error');

                    values.push({
                        email,
                        valid: isValid,
                    });

                    return values;
                }
            );
        });

        expect(await resultCheck).to.deep.equal(
            VALUES,
            `Значения валидности не совпали с заданным. Должно быть: ${JSON.stringify(
                VALUES
            )}, по факту: ${JSON.stringify(await resultCheck)}`
        );

        await cancelButton.clickButton();

        await this.browser.waitUntil(alertIsPresent(), {
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Не дождались модалки подтверждения ухода со страницы',
        });

        await this.browser.acceptAlert();
    });

    it(`Отменить изменения в форме путем клика на кнопку "Отменить", должен произойти редирект на ${PAGE_URL_ROOT}`, async function() {
        const cancelButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="cancel-отменить"]'
        );

        await cancelButton.isDisplayed();

        await cancelButton.clickButton();

        const url = await this.browser.getUrl();
        const currUrl = new URL(url);

        expect(currUrl.pathname).to.equal(PAGE_URL_ROOT, 'Значение в адресной строке не совпало с ожидаемым');
    });
});
