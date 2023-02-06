import { Vendor } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { ReactWrapper } from 'enzyme';
import { MockedApi } from '@yandex-market/mbo-test-utils';

import { VendorForm } from 'src/shared/common-logs/components/VendorForm/VendorForm';
import { VendorSelect } from 'src/shared/common-logs/components/VendorSelect/VendorSelect';
import { AliasMaker } from 'src/shared/services';

import cssSuggestSection from 'src/shared/common-logs/components/SuggestSection/SuggestSection.module.scss';

const CLASS_NAME_PARAM_VALUE = `.${cssSuggestSection.ParamValue}`;

export const selectVendor = (app: ReactWrapper, aliasMaker: MockedApi<AliasMaker>, vendor: Vendor) => {
  app.find(VendorForm).find(VendorSelect).props().onSelect(vendor);

  app.update();

  expect(app.find(VendorForm).find(CLASS_NAME_PARAM_VALUE).text()).toBe(vendor.name);
};
