import RETURN_TEXT_CONSTANTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';
import {replaceBreakChars} from '@self/root/src/spec/utils/text';

export const FIRST_STEP_TITLE = replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_FIRST_STEP_TITLE);
export const SECOND_STEP_TITLE = replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_STEP_SEND_BOX_POST_SIMPLE_RETURN);
export const DROPSHIP_RETURN_CONTACTS_LINK_TEXT = {
    SHOWN: 'Свернуть',
    HIDDEN: 'Другие способы возврата',
};

export const getFirstStepTexts = ({orderId, isDsbs = false}) => {
    let texts = [
        `${orderId} — проверьте, что написали цифры разборчиво и без ошибок. ` +
        'Иначе посылка может потеряться, а вернуть деньги не получится.',

        'Положите товар и листок с номером в коробку, пакет или конверт и плотно запечатайте. ' +
        'Если у вас нет подходящей упаковки, её можно купить прямо в почтовом отделении.',
    ];

    if (isDsbs) {
        texts.push(
            'Обратите внимание на габариты посылки. Её могут не принять на почте, если она тяжелее 20 кг, ' +
            'сумма длины, ширины и высоты больше 300 см или одна из сторон больше 150 см.'
        );
    }

    return texts;
};

export const getSecondStepTexts = postAddress => ([
    `Адрес отделения: ${postAddress}`,
    '1. Скажите сотруднику почты, что возвращаете товар с Яндекс.Маркета.',
    '2. Назовите трек-номер возврата — его мы пришлём в смс и письме.',
]);
