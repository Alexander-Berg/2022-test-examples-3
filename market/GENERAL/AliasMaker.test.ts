import { Action, OfferType } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { Scope } from '@yandex-market/market-proto-dts/Market/Mboc/ContentCommentTypes';

import { TaskType } from 'src/shared/common-logs/helpers/types';
import { RUSSIAN_LANG_ID } from 'src/shared/constants';
import { AliasMaker } from './AliasMaker';

const getRequestType = (path?: string) => path?.split('?')[0]?.split('/').pop();

const checkGetRequest = (request: { path: string; data: any }, id: string, query: string) => {
  expect(getRequestType(request?.path)).toEqual(id);
  expect(request?.data.method).toEqual('GET');
  expect(request?.data.data.q).toEqual(query);
  expect(typeof request?.data.data.reqId).toEqual('string');
  expect(typeof request?.data.data.uid).toEqual('string');
};

const checkPostRequest = (request: { path: string; data: any }, id: string, query: string) => {
  expect(getRequestType(request?.path)).toEqual(id);
  expect(request?.data.method).toEqual('POST');
  expect(request?.data.data).toEqual(query);
  expect(request?.path).toContain('reqId=');
  expect(request?.path).toContain('uid=');
};

describe('AliasMaker', () => {
  describe('inits', () => {
    it('with minimal data', () => {
      const proxy = jest.fn();
      const am = new AliasMaker(proxy, 'test');
      expect(am).toBeDefined();
      expect(am.assignmentId).toEqual('test');
      expect(am.proxy).toEqual(proxy);
      expect(am.offerType).toEqual(OfferType.SUPPLIER_OFFER);
    });

    it('with task type', () => {
      const proxy = jest.fn();
      const am = new AliasMaker(proxy, 'test', TaskType.MSKU_FROM_PSKU_GENERATION);
      expect(am.offerType).toEqual(OfferType.PSKU);
    });
  });

  describe('public methods', () => {
    let lastRequest: { path: string; data: any } | undefined;

    const proxy = (path: string, data: any) => {
      lastRequest = { path, data };

      return Promise.resolve({ reqId: data.reqId }) as unknown as JQueryPromise<any>;
    };

    beforeEach(() => {
      lastRequest = undefined;
    });

    const am = new AliasMaker(proxy, 'test', TaskType.BLUE_LOGS, 'testTaskId');
    it('RequestStatisticSubject works', async () => {
      const statSubject = am.getRequestStatisticSubject();
      expect(statSubject).toBeDefined();

      let lastSubjData: any;

      statSubject.subscribe(data => {
        lastSubjData = data;
      });

      await am.getVendors({});

      expect(typeof lastSubjData?.req_id).toEqual('string');
      expect(lastSubjData?.url).toEqual('/newBalanserProxy/GetVendors');
      expect(lastSubjData?.duration_ms).toBeDefined();
    });

    it('addParamValue works', async () => {
      await am.addParamValue({
        category_id: 123,
        param_id: 234,
        option: [
          {
            name: [
              {
                name: 'testName',
                lang_id: RUSSIAN_LANG_ID,
              },
            ],
          },
        ],
      });

      checkPostRequest(
        lastRequest!,
        'AddParamValue',
        '{"category_id":123,"param_id":234,"option":[{"name":[{"name":"testName","lang_id":225}]}],"assignment_id":"test","task_id":"testTaskId"}'
      );
    });

    it('searchModels works', async () => {
      await am.searchModels({
        category_id: 123,
      });

      checkPostRequest(lastRequest!, 'SearchModels', '{"category_id":123,"query":"","limit":25,"offset":0}');

      await am.searchModels({
        category_id: 123,
        query: 'test',
        limit: 42,
        offset: 24,
      });

      checkPostRequest(lastRequest!, 'SearchModels', '{"category_id":123,"query":"test","limit":42,"offset":24}');
    });

    it('searchParamValue works', async () => {
      await am.searchParamValue({
        category_id: 123,
      });

      checkPostRequest(lastRequest!, 'SearchParamValue', '{"category_id":123,"query":"","limit":25,"offset":0}');

      await am.searchParamValue({
        category_id: 123,
        query: 'test',
        limit: 42,
        offset: 24,
      });

      checkPostRequest(lastRequest!, 'SearchParamValue', '{"category_id":123,"query":"test","limit":42,"offset":24}');
    });

    it('searchVendors works', async () => {
      await am.searchVendors({
        category_id: 123,
      });

      checkPostRequest(lastRequest!, 'SearchVendors', '{"category_id":123,"query":"","limit":50,"offset":0}');

      await am.searchVendors({
        category_id: 123,
        query: 'test',
        limit: 42,
        offset: 24,
      });

      checkPostRequest(lastRequest!, 'SearchVendors', '{"category_id":123,"query":"test","limit":42,"offset":24}');
    });

    it('updateModel works', async () => {
      await am.updateModel({});

      checkPostRequest(
        lastRequest!,
        'UpdateModel',
        '{"offer_type":"SUPPLIER_OFFER","async_update_matcher":true,"assignment_id":"test","task_id":"testTaskId"}'
      );
    });

    it('updateVendor works', async () => {
      await am.updateVendor({});

      checkPostRequest(
        lastRequest!,
        'UpdateVendor',
        '{"offer_type":"SUPPLIER_OFFER","async_update_matcher":true,"assignment_id":"test","task_id":"testTaskId"}'
      );
    });

    it('getModels works', async () => {
      await am.getModels({ category_id: 123, model_type: 'GURU' });

      checkPostRequest(lastRequest!, 'GetModels', '{"category_id":123,"model_type":"GURU"}');
    });

    it('findModels works', async () => {
      await am.findModels({ category_id: 123 });

      checkGetRequest(lastRequest!, 'FindModels', '{"category_id":123}');
    });

    it('getImagesFromSearch works', async () => {
      await am.getImagesFromSearch({});

      checkGetRequest(lastRequest!, 'GetImagesFromSearch', '{"limit":25,"offset":0}');

      await am.getImagesFromSearch({
        query: 'test',
        limit: 42,
        offset: 24,
      });

      checkGetRequest(lastRequest!, 'GetImagesFromSearch', '{"query":"test","limit":42,"offset":24}');
    });

    it('getImagesFromOffer works', async () => {
      await am.getImagesFromOffer({});

      checkGetRequest(lastRequest!, 'GetImagesFromOffer', '{"limit":25,"offset":0}');

      await am.getImagesFromOffer({
        limit: 42,
        offset: 24,
      });

      checkGetRequest(lastRequest!, 'GetImagesFromOffer', '{"limit":42,"offset":24}');
    });

    it('getUpdatedMatching works', async () => {
      await am.getUpdatedMatching({});

      checkPostRequest(lastRequest!, 'UpdateTask', '{"offer_type":"SUPPLIER_OFFER","assignment_id":"test"}');
    });

    it('updateModelsGroup works', async () => {
      await am.updateModelsGroup({});

      checkPostRequest(
        lastRequest!,
        'UpdateModelsGroup',
        '{"offer_type":"SUPPLIER_OFFER","action":"UPDATE","replace_pictures":true,"async_update_matcher":true,"assignment_id":"test","task_id":"testTaskId","yang_task_type":"YANG_TASK_BLUE_LOGS"}'
      );

      await am.updateModelsGroup({ action: Action.REMOVE, replace_pictures: false });

      checkPostRequest(
        lastRequest!,
        'UpdateModelsGroup',
        '{"offer_type":"SUPPLIER_OFFER","action":"REMOVE","replace_pictures":false,"async_update_matcher":true,"assignment_id":"test","task_id":"testTaskId","yang_task_type":"YANG_TASK_BLUE_LOGS"}'
      );
    });

    it('uploadImages works', async () => {
      await am.uploadImages({ local_image: [{ content_base64: 'test' }], remote_url: ['test1'] });

      checkPostRequest(
        lastRequest!,
        'UploadImages',
        '{"local_image":[{"content_base64":"test"}],"remote_url":["test1"]}'
      );
    });

    it('searchMappingsByMarketSkuId works', async () => {
      await am.searchMappingsByMarketSkuId({});

      checkPostRequest(
        lastRequest!,
        'SearchMappingsByMarketSkuId',
        '{"mapping_kind":["APPROVED_MAPPING"],"supplier_types":["TYPE_FIRST_PARTY","TYPE_THIRD_PARTY","TYPE_REAL_SUPPLIER","TYPE_FMCG"]}'
      );
    });

    it('getModelsExported works', async () => {
      await am.getModelsExported({ category_id: 123, model_type: 'GURU' });

      checkGetRequest(lastRequest!, 'GetModelsExported', '{"category_id":123,"model_type":"GURU"}');
    });

    it('getVendors works', async () => {
      await am.getVendors({ category_id: 123 });

      checkGetRequest(lastRequest!, 'GetVendors', '{"category_id":123}');
    });

    it('getSizeMeasuresInfo works', async () => {
      await am.getSizeMeasuresInfo({ category_ids: [123] });

      checkGetRequest(lastRequest!, 'GetSizeMeasuresInfo', '{"category_ids":[123]}');
    });

    it('getSizeMeasuresVendorsInfo works', async () => {
      await am.getSizeMeasuresVendorsInfo({ vendor_requests: [{ category_id: 123, vendor_ids: [234] }] });

      checkGetRequest(
        lastRequest!,
        'GetSizeMeasuresVendorsInfo',
        '{"vendor_requests":[{"category_id":123,"vendor_ids":[234]}]}'
      );
    });

    it('getContentCommentTypes works', async () => {
      await am.getContentCommentTypes({ scope: Scope.CLASSIFICATION });

      checkGetRequest(lastRequest!, 'GetContentCommentTypes', '{"scope":"CLASSIFICATION"}');
    });

    it('suspendCategorySupplierVendor works', async () => {
      await am.suspendCategorySupplierVendor({ category_id: 123, supplier_id: 234, vendor_id: 345 });

      checkPostRequest(
        lastRequest!,
        'SuspendCategorySupplierVendor',
        '{"category_id":123,"supplier_id":234,"vendor_id":345}'
      );
    });

    it('getCategoryCutOffWords works', async () => {
      await am.getCategoryCutOffWords({ category_id: 123 });

      checkGetRequest(lastRequest!, 'GetCategoryCutOffWords', '{"category_id":123}');
    });

    it('updateCutOffWords works', async () => {
      await am.updateCutOffWords({ category_id: 123 });

      checkPostRequest(
        lastRequest!,
        'UpdateCutOffWord',
        '{"category_id":123,"offer_type":"SUPPLIER_OFFER","async_update_matcher":true,"assignment_id":"test","task_id":"testTaskId"}'
      );
    });

    it('checkCutOffWords works', async () => {
      await am.checkCutOffWords({ category_id: 123, word: 'test' });

      checkGetRequest(lastRequest!, 'CheckCutOffWord', '{"category_id":123,"word":"test"}');
    });

    it('getModelForms works', async () => {
      await am.getModelForms({ category_ids: [123] });

      checkGetRequest(lastRequest!, 'GetModelForms', '{"category_ids":[123]}');
    });

    it('getParameters works', async () => {
      await am.getParameters({ category_id: 123 });

      checkGetRequest(lastRequest!, 'GetParameters', '{"category_id":123,"timestamp":-1}');
    });

    it('getCategoryRuleSet works', async () => {
      await am.getCategoryRuleSet({ category_id: [123] });

      checkGetRequest(lastRequest!, 'GetCategoryRuleSet', '{"category_id":[123]}');
    });

    it('getAuditActions works', async () => {
      await am.getAuditActions({ category_id: 123 });

      checkGetRequest(lastRequest!, 'YangGetAuditActions', '{"category_id":123,"task_id":"testTaskId"}');
    });

    it('sendStatistics works', async () => {
      await am.sendStatistics({ events: [{ name: 'test' }] });

      expect(getRequestType(lastRequest!.path)).toEqual('RecordFrontendStat');
      expect(lastRequest!.data.method).toEqual('POST');
      expect(lastRequest!.path).toContain('reqId=');
      expect(lastRequest!.path).toContain('uid=');

      expect(lastRequest!.data.data).toContain('"events":[{"name":"test"}]');
      expect(lastRequest!.data.data).toContain('"page":"BLUE_LOGS"');
      expect(lastRequest!.data.data).toContain('"context":"test"');
    });

    it('sendActivityPing works', async () => {
      await am.sendActivityPing();

      checkPostRequest(lastRequest!, 'YangWorkingTimePing', '{"assignment_id":"test","ping_interval_sec":120}');
    });
  });

  describe('failure handling', () => {
    it('works', async () => {
      const proxy = () => {
        // eslint-disable-next-line prefer-promise-reject-errors
        return Promise.reject({}) as unknown as JQueryPromise<any>;
      };

      const am = new AliasMaker(proxy, 'test', TaskType.BLUE_LOGS, 'testTaskId');

      try {
        await am.getVendors({});
      } catch (e: any) {
        expect(typeof e?.reqId).toEqual('string');
      }
    });
  });
});
