import type {DatasourceInfo, GetDatasourceResult} from '~/app/bcm/mbiPartner/Client/DatasourceClient/types';
import type {PartnerManager} from '~/app/entities/manager/types';
import {PlacementType} from '~/app/entities/placement/types';
import {ManagerType} from '~/app/entities/manager/types';

type Params =
    | undefined
    | Partial<{
          datasourceInfo: Partial<DatasourceInfo>;
          partnerManager: Partial<PartnerManager>;
      }>;

export default ({datasourceInfo, partnerManager}: Params = {}): GetDatasourceResult => ({
    datasourceInfo: {
        id: 100,
        domain: 'yandex.ru',
        internalName: 'Yandex test',
        managerId: 3,
        placementTypes: [PlacementType.Dropship],
        ...datasourceInfo,
    },
    partnerManager: {
        id: 3,
        name: 'Аркадий Волож',
        email: 'volozh@yandex-team.ru',
        login: 'volozh',
        hosted: false,
        managerType: ManagerType.Yandex,
        passportEmail: 'volozh@yandex-team.ru',
        ...partnerManager,
    },
});
