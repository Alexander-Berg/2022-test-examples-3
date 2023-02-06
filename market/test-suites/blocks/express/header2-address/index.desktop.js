import {prepareSuite} from 'ginny';
import ExpressAddressPopup from '@self/root/src/widgets/content/ExpressAddressPopup/components/View/__pageObject';
import AddressMapEdit from '@self/root/src/widgets/content/ExpressAddressPopup/components/AddressMapEdit/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import PinMap from '@self/root/src/components/PinMap/__pageObject';

import Header2AddressSuite from '.';

export default prepareSuite(Header2AddressSuite, {
    hooks: {
        async beforeEach() {
            this.setPageObjects({
                expressPopup: () => this.createPageObject(ExpressAddressPopup),
                addressMapEdit: () => this.createPageObject(AddressMapEdit),
                addressSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressMapEdit,
                }),
                addressPinMap: () => this.createPageObject(PinMap, {
                    parent: this.addressMapEdit,
                }),
            });
        },
    },
});
