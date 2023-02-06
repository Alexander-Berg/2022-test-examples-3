import {YANDEX_PLUS_ONBOARDING_TYPE} from '@self/root/src/constants/yaPlus';
import {MARKET_CASHBACK_PERCENT} from '@self/root/src/entities/perkStatus/perks/yandexCashback';

// PageObjects
import YaPlusPopupContent from '@self/root/src/components/YaPlusPopupContent/__pageObject';
import Title from '@self/root/src/uikit/components/Title/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

import {showPopupAction, mockWidgetData} from './mockData';
import {initContext} from './helpers';

const widgetPath = '@self/root/src/widgets/content/YandexPlusOnboarding';

const POPUP_LINK = 'https://plus.yandex.ru/?utm_source=market&utm_medium=banner&utm_campaign=MSCAMP-77&utm_term=src_market&utm_content=onboarding&message=market';

const createStepsConfig = (isPlusPromoAvailable, onboardingType) => {
    let steps = [];
    switch (onboardingType) {
        // Неплюсовик, с нулевым балансом
        case YANDEX_PLUS_ONBOARDING_TYPE.NON_YA_PLUS_WITHOUT_BALANCE:
            steps = [{
                title: 'С кешбэком Плюса можно покупать товары всего за 1 ₽ ',
                text: 'А ещё экономить на покупках, поездках<br>и развлечениях в других сервисах&nbsp;Яндекса.',
                buttonText: 'Неплохо, а как?',
            }, {
                title: 'Просто выбирайте товарысо значком ',
                text: `До ${MARKET_CASHBACK_PERCENT}% стоимости вернётся баллами.<br>Всё честно: 1 балл это 1 рубль.`,
                buttonText: 'А дальше?',
            }, {
                title: isPlusPromoAvailable
                    ? 'А чтобы тратить баллы, подключите Яндекс Плюс'
                    : 'Подключите Яндекс Плюс,чтобы тратить баллы',
                text: isPlusPromoAvailable
                    ? 'Приятный бонус — ещё и доставка товаров от 499&nbsp;₽ станет&nbsp;бесплатной'
                    : 'И оплачивать ими до 99% стоимости&nbsp;покупки',
                buttonText: 'Пойду подключу',
            }];
            break;
        // Неплюсовик, с балансом
        case YANDEX_PLUS_ONBOARDING_TYPE.NON_YA_PLUS_WITH_BALANCE:
            steps = [{
                title: 'Баллы ждут,когда их потратят',
                text: 'Их можно списывать на Маркете<br> и покупать товары всего по 1&nbsp;₽',
                buttonText: 'Я не против, а как?',
            }, {
                title: isPlusPromoAvailable
                    ? 'Нужно толькоподключить Плюс'
                    : 'Подключите Яндекс Плюс,чтобы тратить баллы',
                text: isPlusPromoAvailable
                    ? 'Приятный бонус — ещё и доставка<br>товаров от 499&nbsp;₽ станет бесплатной'
                    : 'И оплачивать ими до 99% стоимости&nbsp;покупки',
                buttonText: 'Пойду подключу',
            }];
            break;
        // Неплюсовик со сгорающим кешбэком
        case YANDEX_PLUS_ONBOARDING_TYPE.CASHBACK_ANNIHILATION:
            steps = [{
                title: '100 баллов Плюса сгорят завтра',
                text: 'Яндекс Плюс их сохранит. Ещё вы сможете получать новые баллы и&nbsp;тратить их&nbsp;в&nbsp;сервисах Яндекса: на Маркете, в Лавке, Такси и&nbsp;других. 1&nbsp;балл&nbsp;=&nbsp;1&nbsp;рубль.',
                buttonText: 'Подключить Плюс',
                buttonType: 'action',
                skipButtonText: 'Подробнее про Плюс',
                skipButtonLink: POPUP_LINK,
            }];
            break;
        // Плюсовик без баланса
        case YANDEX_PLUS_ONBOARDING_TYPE.YA_PLUS_WITHOUT_BALANCE:
            steps = [{
                title: 'Вы в Плюсе — копите и тратьте баллы',
                text: 'Списывайте их на Маркете и в других сервисах Яндекса.<br>Так, 300&nbsp;баллов&nbsp;— скидка 300&nbsp;₽.',
                buttonText: isPlusPromoAvailable
                    ? 'А что с доставкой?'
                    : 'Здорово, а ещё?',
            }];
            if (isPlusPromoAvailable) {
                steps.push({
                    title: 'А за доставкувам платить не нужно',
                    text: 'Курьер бесплатно привезёт<br>заказы от&nbsp;499&nbsp;₽ — зависит от города',
                    buttonText: 'Здорово, а ещё?',
                });
            }
            break;
        case YANDEX_PLUS_ONBOARDING_TYPE.YA_PLUS_WITH_BALANCE:
            steps = [{
                title: 'Ваши баллы Плюса ждут, когда их потратят',
                text: 'Списывайте их на Маркете и в других сервисах Яндекса.<br>Так, 300&nbsp;баллов&nbsp;— скидка 300&nbsp;₽.',
                buttonText: isPlusPromoAvailable ? 'А что с доставкой?' : 'Здорово, а ещё?',
            }];
            if (isPlusPromoAvailable) {
                steps.push({
                    title: 'А за доставкувам платить не нужно',
                    text: 'Курьер бесплатно привезёт<br>заказы от&nbsp;499&nbsp;₽ — зависит от города',
                    buttonText: 'Здорово, а ещё?',
                });
            }
            break;
        default:
            steps = [];
            break;
    }
    return steps;
};
export const checkPopupSteps = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockData,
    onboardingType
) => {
    await initContext(mandrelLayer, {});
    await jestLayer.backend.runCode(mockWidgetData, [mockData]);
    await apiaryLayer.mountWidget(widgetPath, {});

    await showPopupAction();

    const stepsConfig = createStepsConfig(mockData.isPlusPromoAvailable, onboardingType);

    // eslint-disable-next-line array-callback-return
    return stepsConfig.map((step, stepId) => {
        const popup = document.body.querySelector(`[data-auto="yaPlusCashbackOnboarding"] ${YaPlusPopupContent.root}`);
        const popupTitle = popup.querySelector(Title.root);
        const popupText = popup.querySelector(Text.root);
        const popupLinks = popup.querySelectorAll(Link.root);
        const popupButton = popupLinks[0];

        expect(popup).not.toBeNull();
        expect(popupTitle.textContent).toEqual(step.title);
        // compare with .textContent doesn't work
        expect(popupText.innerHTML).toEqual(step.text);
        expect(popupButton.textContent).toEqual(step.buttonText);

        if (step.skipButtonText) {
            const skipButton = popupLinks[1];
            expect(skipButton.textContent).toEqual(step.skipButtonText);

            if (step.skipButtonLink) {
                expect(skipButton.getAttribute('href')).toEqual(step.skipButtonLink);
            }
        }

        if (stepId + 1 !== stepsConfig.length) {
            popupButton.click();
        } else if (step.buttonType !== 'action') {
            expect(popupButton.getAttribute('href')).toEqual(POPUP_LINK);
        }
    });
};

export const checkPopupNotShow = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockData
) => {
    await initContext(mandrelLayer, {});
    await jestLayer.backend.runCode(mockWidgetData, [mockData]);
    await apiaryLayer.mountWidget(widgetPath, {});

    await showPopupAction();

    const popup = document.body.querySelector(`[data-auto="yaPlusCashbackOnboarding"] ${YaPlusPopupContent.root}`);
    expect(popup).toBeNull();
};
