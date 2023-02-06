import type {CourierDtoWithAcceptCodeStatus} from '~/app/bcm/ff4Shops/Client/OrdersClient/types';
import {AcceptCodeStatus} from '~/app/constants/orders';

export default (draft?: Partial<CourierDtoWithAcceptCodeStatus>): CourierDtoWithAcceptCodeStatus => ({
    lastName: 'Лихачев',
    firstName: 'Иван',
    middleName: 'Иванович',
    phoneNumber: null,
    phoneExtension: null,
    vehicleNumber: 'С386КК750',
    vehicleDescription: 'Вишневая девятка',
    url: null,
    electronicAcceptanceCertificateCode: '000000',
    electronicAcceptCodeRequired: true,
    electronicAcceptCodeStatus: AcceptCodeStatus.OK_HIDE,
    courierType: 'CAR',
    ...draft,
});
