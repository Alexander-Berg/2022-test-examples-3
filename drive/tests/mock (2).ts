import { buttonLocationDetails } from '../../../utils/sendLogs/eventTypes/buttonDetails';
import { tagTypes } from '../../InterfaceAdminConfig/FastTags/schema';

const data = [{
    'enabled': true,
    'tag': 'DTP_registration',
    'place': [],
    'alias': 'ДТП: оформление',
}, { 'enabled': false, 'tag': 'wheel_damaged', 'place': ['car_card', 'client_card'], 'alias': 'Колесо' }, {
    'enabled': true,
    'tag': 'engine_oil',
    'place': ['client_card'],
    'alias': 'Масло',
}, { 'enabled': true, 'tag': 'fueling_washing_liquid', 'place': ['car_card'], 'alias': 'Омывайка' }];

const dataClientInfo = [{
    'enabled': true,
    'tag': 'письмо о штрафе',
    'place': buttonLocationDetails.CLIENT_INFO,
    'alias': 'письмо о штрафе',
    'tagType': tagTypes.CLIENT,
}];

export { data, dataClientInfo };
