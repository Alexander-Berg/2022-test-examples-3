import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import firstStepDeliveryAndCash from './firstStep';
import secondStepDeliveryAndCash from './secondStep';
import thirdStepDeliveryAndCash from './thirdStep';

export default makeSuite('В корзине фарма FBS Express + фешн товар с примеркой', {
    feature: 'Первая покупка',
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(firstStepDeliveryAndCash),
        prepareSuite(secondStepDeliveryAndCash),
        prepareSuite(thirdStepDeliveryAndCash)
    ),
});
