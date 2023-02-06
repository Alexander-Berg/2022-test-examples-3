import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import medicalFBSExpressAndDBS from './delivery/medicalFBSExpressAndDBS';
import medicalExpressAndFashion from './delivery/medicalExpressAndFashion';
import onlyMedicalCart from './delivery/onlyMedicalCart';

export default makeSuite('Покупка списком. Повторная покупка', {
    feature: 'Повторная покупка',
    environment: 'kadavr',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        prepareSuite(medicalFBSExpressAndDBS),
        prepareSuite(medicalExpressAndFashion),
        prepareSuite(onlyMedicalCart)
    ),
});
