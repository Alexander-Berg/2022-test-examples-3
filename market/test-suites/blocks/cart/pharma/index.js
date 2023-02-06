import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import badgesInPharmaCart from './badgesInPharmaCart';
import pharmaInCart from './pharmaInCart';

export default makeSuite('Фарма', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(pharmaInCart, {}),
        prepareSuite(badgesInPharmaCart, {})
    ),
});
