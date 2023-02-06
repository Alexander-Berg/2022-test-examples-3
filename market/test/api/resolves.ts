import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { Api } from 'src/java/Api';
import {
  ImportResult,
  SaveParamsMappingWithRulesDiffResponse,
  DeleteMappingsResponse,
  ProcessingKey,
  ProcessingResult,
  ShopModelView,
  ShopCategoryStatisticsItem,
  MappingsAndRules,
  CategoryInfo,
  UserInfo,
  CategoryData,
  ShopView,
  ParamFormalizerResponse,
} from 'src/java/definitions';
import { userInfo, shops, categoryInfo, categoryStat } from '../data';
import { act } from '@testing-library/react';

export const resolveRemoveMappingRequest = (api: MockedApiObject<Api>, response: DeleteMappingsResponse) => {
  act(() => {
    api.paramMappingControllerV2.deleteMapping.next().resolve(response);
  });
};

export const resolveLoadMappingRequest = (api: MockedApiObject<Api>, response: MappingsAndRules) => {
  act(() => {
    api.paramMappingController.getMappingsByShopId.next().resolve(response);
  });
};

export const resolveLoadShopsRequest = (api: MockedApiObject<Api>, response: ShopView[]) => {
  act(() => {
    api.shopControllerV2.userShops.next().resolve(response);
  });
};

export const resolveUserInfoRequest = (api: MockedApiObject<Api>, response: UserInfo) => {
  act(() => {
    api.userController.getInfo.next().resolve(response);
  });
};

export const resolveLoadCategoryTreeRequest = (api: MockedApiObject<Api>, response: CategoryInfo[]) => {
  act(() => {
    api.categoryTreeController.getCategoryTree.next().resolve(response);
  });
};

export const resolveLoadCategoryDataRequest = (api: MockedApiObject<Api>, response: CategoryData) => {
  act(() => {
    api.categoryDataController.categoryData.next().resolve(response);
  });
};

export const resolveLoadModelsRequest = (api: MockedApiObject<Api>, response: ShopModelView[]) => {
  act(() => {
    api.shopModelController.loadAllModelsByShopIdV3.next().resolve(response);
  });
};

export const resolveLoadStatisticsRequest = (api: MockedApiObject<Api>, response: ShopCategoryStatisticsItem[]) => {
  act(() => {
    api.shopModelController.getShopCategoriesStatistics.next().resolve(response);
  });
};

export const resolveSaveMappingRequest = (
  api: MockedApiObject<Api>,
  response: SaveParamsMappingWithRulesDiffResponse
) => {
  act(() => {
    api.paramMappingControllerV2.saveParamMappingWithRulesDiff.next().resolve(response);
  });
};

export const resolveFormalizeMappingRequest = (api: MockedApiObject<Api>, response: ParamFormalizerResponse) => {
  act(() => {
    api.formalizerController.formalizeAndUpdateMapping.next().resolve(response);
  });
};

export const resolveSaveModelsRequest = (api: MockedApiObject<Api>, response: ShopModelView[]) => {
  act(() => {
    api.shopModelController.updateModelsV2.next().resolve(response);
  });
};

export const resolveStartImportRequest = (api: MockedApiObject<Api>, taskId: number) => {
  act(() => {
    api.fileProcessingController.startImport.next().resolve({ taskId });
  });
};

export const resolveCheckUploadRequest = (api: MockedApiObject<Api>, result: ProcessingResult<ImportResult>) => {
  act(() => {
    api.fileProcessingController.checkUpload.next().resolve(result);
  });
};

export const resolveContinuerImportRequest = (api: MockedApiObject<Api>, processingKey: ProcessingKey) => {
  act(() => {
    api.fileProcessingController.continueImport.next().resolve(processingKey);
  });
};

export const resolveDefaultData = (api: MockedApiObject<Api>) => {
  resolveUserInfoRequest(api, userInfo);
  resolveLoadShopsRequest(api, shops);
  resolveLoadCategoryTreeRequest(api, [categoryInfo]);
  resolveLoadStatisticsRequest(api, [categoryStat]);
};
