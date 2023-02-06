import {URLs} from 'constants/urls';

import {accountURLs} from 'projects/account/utilities/urls';

const testId = '12asd312ssdf312DSd3123123';

describe('getHotelsOrderUrl(id)', () => {
    test('Возвращает ссылку на страницу happy page для отелей', () => {
        expect(accountURLs.getHotelsOrderUrl(testId)).toEqual(
            `${URLs.hotelsBookSuccess}/?orderId=${testId}`,
        );
    });
});
