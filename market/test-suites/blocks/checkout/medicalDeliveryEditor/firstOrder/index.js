import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import medicalCartFirstStep from './delivery/medicalExpressAndFashion';
import onlyMedicalCart from './delivery/onlyMedicalCart';

export default makeSuite('Покупка списком. Первая покупка', {
    feature: 'Первая покупка',
    environment: 'kadavr',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        prepareSuite(medicalCartFirstStep),
        prepareSuite(onlyMedicalCart)
    ),
});
