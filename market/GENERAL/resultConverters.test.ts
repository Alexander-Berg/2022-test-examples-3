import { MappingModerationStatus } from 'src/tasks/mapping-moderation/helpers/input-output';
import { ModerationOffer } from 'src/tasks/mapping-moderation/helpers/moderation-types';
import {
  getPskuModerationResult,
  getOfferModerationResult,
  getGskuModerationResult,
  getMmToPskuModerationResult,
} from './resultConverters';

describe('resultConverters', () => {
  describe('getPskuModerationResult', () => {
    it('minimal', () => {
      expect(
        getPskuModerationResult({ id: 1, targetSkuId: 11, generatedSkuId: 21 } as ModerationOffer, {
          status: MappingModerationStatus.ACCEPTED,
        })
      ).toEqual({
        generated_sku_id: 21,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.ACCEPTED,
      });
    });
    it('with deleted gsku', () => {
      expect(
        getPskuModerationResult(
          { id: 1, targetSkuId: 11, generatedSkuId: 21, isDeletedGeneratedSku: true } as ModerationOffer,
          {
            status: MappingModerationStatus.ACCEPTED,
          }
        )
      ).toEqual({
        deleted: true,
        generated_sku_id: 21,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.UNDEFINED,
      });
    });
    it('with deleted target sku', () => {
      expect(
        getPskuModerationResult(
          { id: 1, targetSkuId: 11, generatedSkuId: 21, isDeletedTargetSku: true } as ModerationOffer,
          {
            status: MappingModerationStatus.ACCEPTED,
          }
        )
      ).toEqual({
        deleted: false,
        generated_sku_id: 21,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.REJECTED,
      });
    });
    it('with  comments', () => {
      expect(
        getPskuModerationResult({ id: 1, targetSkuId: 11, generatedSkuId: 21 } as ModerationOffer, {
          status: MappingModerationStatus.REJECTED,
          contentComments: [{ type: 'test', items: ['test'] }],
        })
      ).toEqual({
        generated_sku_id: 21,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.REJECTED,
        content_comment: [{ type: 'test', items: ['test'] }],
      });
    });
  });
  describe('getOfferModerationResult', () => {
    it('minimal', () => {
      expect(
        getOfferModerationResult({ id: 1, targetSkuId: 11, offer: { offer_id: '31' } } as ModerationOffer, {
          status: MappingModerationStatus.ACCEPTED,
        })
      ).toEqual({
        offer_id: 31,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.ACCEPTED,
      });
    });
    it('with comments', () => {
      expect(
        getOfferModerationResult({ id: 1, targetSkuId: 11, offer: { offer_id: '31' } } as ModerationOffer, {
          status: MappingModerationStatus.REJECTED,
          contentComments: [{ type: 'test', items: ['test'] }],
        })
      ).toEqual({
        offer_id: 31,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.REJECTED,
        content_comment: [{ type: 'test', items: ['test'] }],
      });
    });
  });
  describe('getGskuModerationResult', () => {
    it('minimal', () => {
      expect(
        getGskuModerationResult({ id: 1, targetSkuId: 11, generatedSkuId: 21 } as ModerationOffer, {
          status: MappingModerationStatus.ACCEPTED,
        })
      ).toEqual({
        generated_sku_id: 21,
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.ACCEPTED,
      });
    });
  });
  describe('getMmToPskuModerationResult', () => {
    it('minimal', () => {
      expect(
        getMmToPskuModerationResult(
          { id: 1, targetSkuId: 11, offer: { offer_id: '31' } } as ModerationOffer,
          {
            status: MappingModerationStatus.ACCEPTED,
          },
          { 11: { modifiedDate: 123 } as any }
        )
      ).toEqual({
        offer_id: '31',
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.ACCEPTED,
        sku_modified_ts: 123,
      });
    });
    it('with bad card', () => {
      expect(
        getMmToPskuModerationResult(
          { id: 1, targetSkuId: 11, offer: { offer_id: '31' } } as ModerationOffer,
          {
            status: MappingModerationStatus.REJECTED,
            badCard: true,
            badCardComments: [{ type: 'test', items: ['test'] }],
          },
          { 11: { modifiedDate: 123 } as any }
        )
      ).toEqual({
        offer_id: '31',
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.REJECTED,
        bad_card: true,
        card_comment: [{ type: 'test', items: ['test'] }],
        sku_modified_ts: 123,
      });
    });
    it('with bad card and comment', () => {
      expect(
        getMmToPskuModerationResult(
          { id: 1, targetSkuId: 11, offer: { offer_id: '31' } } as ModerationOffer,
          {
            status: MappingModerationStatus.REJECTED,
            badCard: true,
            contentComments: [{ type: 'test2', items: ['test2'] }],
            badCardComments: [{ type: 'test', items: ['test'] }],
          },
          { 11: { modifiedDate: 123 } as any }
        )
      ).toEqual({
        offer_id: '31',
        msku: 11,
        req_id: 1,
        status: MappingModerationStatus.REJECTED,
        bad_card: true,
        content_comment: [{ type: 'test2', items: ['test2'] }],
        card_comment: [{ type: 'test', items: ['test'] }],
        sku_modified_ts: 123,
      });
    });
  });
});
