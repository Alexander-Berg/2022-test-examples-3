/* eslint-disable @typescript-eslint/explicit-function-return-type */
import {
  DisplayOffer,
  ContentLabState,
  ProcessingStatus,
  SkuType,
  MappingStatus,
  AcceptanceStatus,
} from 'src/java/definitions';

const testOfferIdBase = 8869;

let testOfferId = testOfferIdBase;

const mapping = { mappingId: 1, name: '', timestamp: '133', skuType: SkuType.MARKET, instant: '123' };
const promo = { fromDate: '', price: 1, toDate: '' };

export const testDisplayOffer = (offer: Partial<DisplayOffer> = {}): DisplayOffer => {
  const shopSkuKey = offer.shopSkuKey || { supplierId: 1, shopSku: 'shopSku' };
  const nextOfferId = testOfferId++;
  return {
    id: nextOfferId,
    businessOfferId: nextOfferId,
    acceptanceStatus: AcceptanceStatus.NEW,
    created: Date.now().toString(),
    shopCategoryName: 'Категория shopCategoryName',
    shopSku: shopSkuKey.shopSku,
    businessId: shopSkuKey.supplierId,
    supplierId: shopSkuKey.supplierId,
    shopSkuKey,
    businessSkuKey: { businessId: shopSkuKey.supplierId, shopSku: shopSkuKey.shopSku },
    title: 'title',
    updated: Date.now().toString(),
    acceptanceStatusModified: '',
    approvedSkuMapping: mapping,
    contentLabState: ContentLabState.CL_CONTENT,
    contentSkuMapping: mapping,
    automaticClassification: false,
    barCode: '',
    buyPromo: promo,
    contentComment: '',
    contentComments: [],
    contentLabMessage: 's',
    createdByLogin: '',
    golden: false,
    lastVersion: 1,
    marketCategoryName: '',
    marketModelName: '',
    picUrls: '',
    processingStatus: ProcessingStatus.AUTO_PROCESSED,
    processingStatusModified: '',
    suggestSkuMapping: mapping,
    supplierSkuMapping: mapping,
    supplierSkuMappingStatus: MappingStatus.NEW,
    ticketDeadline: '',
    trackerTicket: '',
    urls: [],
    vendor: 'string',
    vendorCode: 'string',
    allDisplayServiceOffers: [],
    overridenDeadline: '',
    ...offer,
  };
};
