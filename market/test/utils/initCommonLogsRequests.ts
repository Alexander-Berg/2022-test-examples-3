import { Offer, OfferMatchState, OperationStatus, Vendor } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { Model, ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { Category } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { GetSizeMeasuresInfoResponse } from '@yandex-market/market-proto-dts/Market/Mbo/SizeMeasures';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor/es/entities/protoModel/types';
import { ReactWrapper } from 'enzyme';
import { MockedApi } from '@yandex-market/mbo-test-utils';

import { InputData } from 'src/shared/common-logs/helpers/types';
import { AliasMaker } from 'src/shared/services';
import { Mappings } from 'src/shared/common-logs/services/StorageManager';
import { contentCommentTypes } from 'src/shared/test-data/content-comment-types';
import { testCategoryProto } from 'src/shared/test-data/test-categories';
import { testModelForms } from 'src/shared/test-data/test-model-forms';
import { testModelProto } from 'src/shared/test-data/test-models';
import { testVendor } from 'src/shared/test-data/test-vendor';
import { strToInt } from 'src/shared/utils/misc/strToInt';

const success = { status: { status: OperationStatus.SUCCESS } };

const resolveEditorDataRequests = ({
  aliasMaker,
  categoryData,
  sizeMeasuresInfo,
}: {
  aliasMaker: MockedApi<AliasMaker>;
  categoryData: Category;
  sizeMeasuresInfo?: GetSizeMeasuresInfoResponse;
}): void => {
  aliasMaker.getModelForms.next().resolve({
    response: { model_forms: testModelForms },
    ...success,
  });
  aliasMaker.getParameters
    .next(r => r.category_id === categoryData.hid)
    .resolve({ response: { category_parameters: categoryData }, ...success });

  aliasMaker.getContentCommentTypes
    .next()
    .resolve({ response: { content_comment_type: contentCommentTypes }, ...success });

  aliasMaker.getCategoryRuleSet.next().resolve({ rule_set: [] });
  aliasMaker.getSizeMeasuresInfo.next().resolve({ ...success, response: sizeMeasuresInfo || {} });
};

const resolveMatchingRequest = ({
  aliasMaker,
  data,
  isAsyncMatching,
}: {
  aliasMaker: MockedApi<AliasMaker>;
  data: InputData;
  isAsyncMatching?: boolean;
}): void => {
  const matched: OfferMatchState[] = data.logs.map(log => ({
    offer_id: log.offer_id,
    offer: log as unknown as Offer,
  }));

  aliasMaker.getUpdatedMatching
    .next()
    .resolve({ task_offer: matched, ...success, ...(isAsyncMatching ? { reload_state: {} } : undefined) });
};

const resolveHierarchyRequests = ({
  aliasMaker,
  models,
  vendors,
}: {
  aliasMaker: MockedApi<AliasMaker>;
  models: ProtoModel[];
  vendors: Vendor[];
}): void => {
  aliasMaker.getModels.next().resolve({
    model: models,
    result: { status: OperationStatus.SUCCESS },
  });

  aliasMaker.getVendors.next().resolve({
    vendor: vendors,
    result: { status: OperationStatus.SUCCESS },
  });
};

const resolvePostTaskRequests = ({ aliasMaker, data }: { aliasMaker: MockedApi<AliasMaker>; data: InputData }) => {
  aliasMaker.getAuditActions.next().resolve({
    model_actions: [],
    result: { status: OperationStatus.SUCCESS },
  });

  const models: Model[] = data.output!.task_result!.map(r =>
    testModelProto({
      id: r.model_id,
      modelType: ModelType.SKU,
      vendorId: 100,
    })
  );

  const vendors = [testVendor({ vendor_id: 100, name: 'Some Vendor' })];

  resolveHierarchyRequests({ aliasMaker, models, vendors });
};

const resolveModelAndVendorsFromMappingRequests = ({
  aliasMaker,
  mappings,
}: {
  aliasMaker: MockedApi<AliasMaker>;
  mappings: Record<string, Mappings>;
}): void => {
  const models: ProtoModel[] = [];
  const vendors: Vendor[] = [];
  Object.values(mappings).forEach(mapping => {
    const meta = mapping.mapping_meta;
    const vendorId = meta && meta.vendorId;
    const modelId = meta && meta.modelId;
    const skuId = meta && meta.skuId;

    const vendor = vendorId && testVendor({ vendor_id: vendorId, name: 'Some Vendor' });
    if (vendor && !vendors.some(v => v.vendor_id === vendor.vendor_id)) {
      vendors.push(vendor);
    }

    const model =
      modelId &&
      testModelProto({
        id: modelId,
        modelType: ModelType.GURU,
        vendorId,
      });

    if (model && !models.some(m => m.id === model.id)) {
      models.push(model);
    }

    const sku =
      skuId &&
      testModelProto({
        id: skuId,
        modelType: ModelType.SKU,
        vendorId,
      });

    if (sku && !models.some(m => m.id === sku.id)) {
      models.push(sku);
    }
  });

  resolveHierarchyRequests({ aliasMaker, vendors, models });
  aliasMaker.getImagesFromSearch.process(request => {
    request.resolve({
      image: [],
      result: {
        status: OperationStatus.SUCCESS,
      },
    });
  });
};

export const initCommonLogsRequests = ({
  aliasMaker,
  data,
  app,
  categoryData = testCategoryProto({ id: strToInt(data.category_id) }),
  mappings,
  sizeMeasuresInfo,
  isAsyncMatching,
}: {
  aliasMaker: MockedApi<AliasMaker>;
  data: InputData;
  app: ReactWrapper;
  vendor?: Vendor;
  categoryData?: Category;
  mappings?: Record<string, Mappings>;
  sizeMeasuresInfo?: GetSizeMeasuresInfoResponse;
  isAsyncMatching?: boolean;
}) => {
  resolveEditorDataRequests({ aliasMaker, categoryData, sizeMeasuresInfo });
  resolveMatchingRequest({ aliasMaker, data, isAsyncMatching });
  app.update();

  // eslint-disable-next-line no-unused-expressions
  aliasMaker.searchVendors.next()?.resolve({ result: { status: OperationStatus.SUCCESS } });

  // eslint-disable-next-line no-unused-expressions
  aliasMaker.searchVendors.next()?.resolve({ result: { status: OperationStatus.SUCCESS } });

  // non empty localStorage case
  if (mappings) {
    resolveModelAndVendorsFromMappingRequests({ aliasMaker, mappings });
  }

  // post task
  if (data.output) {
    resolvePostTaskRequests({ aliasMaker, data });
  }

  app.update();
};
