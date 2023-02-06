import { ToastContainer } from 'react-toastify';

import { MappingStatuses } from 'src/shared/common-logs/helpers/types';
import { App } from 'src/tasks/common-logs/components/App/App';
import { OfferComponent } from 'src/tasks/common-logs/components/Offer/Offer';
import postTask from 'src/tasks/common-logs/task-suits/test-post-task';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';

describe('initialization', () => {
  it('base', () => {
    const { app } = initCommonLogsApp({ initialData: simpleTask as any });
    expect(app.find(App).length).toBe(1);
    expect(app.find(OfferComponent).length).toBe(1);

    // prettier-ignore
    // eslint-disable-next-line no-useless-escape,prettier/prettier
    expect(app.find(ToastContainer).html()).toEqual('<div class=\"Toastify\"></div>');

    expect(app.find('.CategoryName').text()).toContain('Мобильные телефоны');
  });
  it('with local storage data', () => {
    const { app, aliasMaker } = initCommonLogsApp({
      initialData: simpleTask,
      mappings: {
        '4485677': { map_status: MappingStatuses.MAPPED, mapping_meta: { vendorId: 101, modelId: 1001 } },
        '4485678': { map_status: MappingStatuses.MAPPED, mapping_meta: { vendorId: 100, modelId: 1000 } },
        '4485680': { map_status: MappingStatuses.MAPPED, mapping_meta: { vendorId: 102, modelId: 1002 } },
      },
    });
    expect(aliasMaker.activeRequests()).toBeEmpty();

    expect(app.find(App).length).toBe(1);
    expect(app.find(OfferComponent).length).toBe(1);

    // prettier-ignore
    // eslint-disable-next-line no-useless-escape,prettier/prettier
    expect(app.find(ToastContainer).html()).toEqual('<div class=\"Toastify\"></div>');

    expect(app.find('.CategoryName').text()).toContain('Мобильные телефоны');
    localStorage.clear();
  });
  it('with post data', () => {
    const { app } = initCommonLogsApp({ initialData: postTask as any });

    // prettier-ignore
    // eslint-disable-next-line no-useless-escape,prettier/prettier
    expect(app.find(ToastContainer).html()).toEqual('<div class=\"Toastify\"></div>');
  });
});
