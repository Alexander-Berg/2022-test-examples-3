export interface ITestCard {
    number: string;
    CVC: number;
    validDateYear: number;
    validDateMonth: number;
}

/** Тестовая банковская карта */
export const card: ITestCard = {
    number: '5100002767155462',
    validDateMonth: 12,
    validDateYear: 22,
    CVC: 454,
};

/** Тестовая банковская карта, с ошибкой оплаты */
export const failCard: ITestCard = {
    number: '5100002767155462',
    validDateMonth: 12,
    validDateYear: 22,
    CVC: 228,
};
