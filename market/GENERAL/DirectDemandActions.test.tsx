import { ReactWrapper } from 'enzyme';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { of } from 'ramda';

import Api from 'src/Api';
import { setupTestApp } from 'src/test/setupApp';
import { DirectDemandActions } from './DirectDemandActions';
import { createDemandDTO } from 'src/test/data/demand';
import { suppliers } from 'src/test/data/replenishmentSuppliers';
import { Button } from 'src/components';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

describe('<DirectDemandActions', () => {
  it('should split api called', () => {
    ({ app, api } = setupTestApp('/replenishment/1'));
    api.demandController.demands.next().resolve(of(createDemandDTO({ id: 1, supplier: suppliers[1] })));
    app.update();

    const button = app.find(DirectDemandActions).find(Button).first();
    button.simulate('click');
    expect(api.demandController.splitDemand).toBeCalledTimes(1);
  });

  it('should monoxdoc split api called', () => {
    ({ app, api } = setupTestApp('/replenishment/1'));
    const demandMock = createDemandDTO({ id: 1, supplier: suppliers[1] });
    demandMock.supplyRoute = 'mono_xdoc';
    demandMock.xdocParentDemandId = 100;

    api.demandController.demands.next().resolve(of(demandMock));
    app.update();

    const button = app.find(DirectDemandActions).find(Button).first();
    button.simulate('click');
    expect(api.demandController.splitMonoXdocDemand).toBeCalledTimes(1);
  });
});
