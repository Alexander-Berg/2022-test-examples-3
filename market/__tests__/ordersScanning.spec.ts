import { OrderFlowStatus, PickupOrder } from '@/apollo/generated/graphql';
import {
  DEFAULT_SCANNING_ERROR_TOOLTIP_TEXT,
  ALREADY_SCANNED_ERROR_MESSAGE,
  BARCODE_DOES_NOT_MATCH_ERROR_MESSAGE,
} from '@/constants/scanning';

import { getSetKey } from '../order';
import { processBarcodes } from '../orderScanning';

jest.mock('@/nativeModules/crashlytics', () => ({}));

const set = new Set<string>();
const scanningProcessCache = new Map<string, any>();
const pickupOrders: PickupOrder[] = [
  {
    externalOrderId: '111',
    multiOrderId: '',
    isMultiOrder: false,
    orderFlowStatus: OrderFlowStatus.SortingCenterPrepared,
    places: [{ barcode: 'P111' }],
    ordinalNumber: 1,
  },
  {
    externalOrderId: '222',
    multiOrderId: '',
    isMultiOrder: false,
    orderFlowStatus: OrderFlowStatus.SortingCenterPrepared,
    places: [{ barcode: '2_1' }],
    ordinalNumber: 2,
  },
  {
    externalOrderId: '333',
    multiOrderId: '',
    isMultiOrder: false,
    orderFlowStatus: OrderFlowStatus.SortingCenterPrepared,
    places: [{ barcode: '3_1' }, { barcode: '3_2' }],
    ordinalNumber: 3,
  },
  {
    externalOrderId: '444',
    multiOrderId: '',
    isMultiOrder: false,
    orderFlowStatus: OrderFlowStatus.SortingCenterPrepared,
    places: [{ barcode: '4_1' }, { barcode: '4_2' }],
    ordinalNumber: 4,
  },
];
const ordersMap = new Map<string, PickupOrder>(
  pickupOrders.map(order => [order.externalOrderId, order]),
);
const scannedOrders = new Set<string>(
  pickupOrders.map(({ externalOrderId, places }) =>
    getSetKey(externalOrderId, places?.[0]?.barcode),
  ),
);

const onErrorHandler = (
  message: string = DEFAULT_SCANNING_ERROR_TOOLTIP_TEXT,
  withErrorDisplayMode: boolean = false,
) => {
  scanningProcessCache.set('error', {
    message: message,
    withErrorDisplayMode,
  });
};
const onSuccesHandler = (order: PickupOrder, placeCode?: string) => {
  scanningProcessCache.set('order', {
    order,
    placeCode: placeCode ?? order.externalOrderId,
  });
};

describe('ScanOrdersListScreen/utils/processBarcodes', () => {
  beforeEach(() => {
    scanningProcessCache.clear();
    set.clear();
  });
  it('Найдет посылку из одноместного заказа, так как ее нет в отсканированных', () => {
    processBarcodes(
      ['111', '222'],
      ordersMap,
      set,
      onErrorHandler,
      onSuccesHandler,
    );

    expect(scanningProcessCache.get('order')).toEqual({
      order: ordersMap.get('111'),
      placeCode: '111',
    });
  });

  it('Найдет посылку из одноместного заказа, но она есть в отсканированных', () => {
    processBarcodes(
      ['111', '222'],
      ordersMap,
      scannedOrders,
      onErrorHandler,
      onSuccesHandler,
    );

    expect(scanningProcessCache.get('error')).toEqual({
      withErrorDisplayMode: false,
      message: ALREADY_SCANNED_ERROR_MESSAGE,
    });
  });

  it('Не найдет посылку, так как в codes нет нужного кода заказа', () => {
    set.add('111');
    processBarcodes(
      ['4653465', '45645645'],
      ordersMap,
      set,
      onErrorHandler,
      onSuccesHandler,
    );

    expect(scanningProcessCache.get('error')).toEqual({
      withErrorDisplayMode: true,
      message: BARCODE_DOES_NOT_MATCH_ERROR_MESSAGE,
    });
  });

  it('Не найдет посылку, так как в codes нет нужного кода многоместной посылки', () => {
    set.add('111');
    processBarcodes(
      ['333', '45645645'],
      ordersMap,
      set,
      onErrorHandler,
      onSuccesHandler,
    );

    expect(scanningProcessCache.get('error')).toEqual({
      withErrorDisplayMode: false,
      message: DEFAULT_SCANNING_ERROR_TOOLTIP_TEXT,
    });
  });

  it('Найдет посылку из многоместного заказа', () => {
    processBarcodes(
      ['333', '3_1'],
      ordersMap,
      set,
      onErrorHandler,
      onSuccesHandler,
    );

    expect(scanningProcessCache.get('order')).toEqual({
      order: ordersMap.get('333'),
      placeCode: '3_1',
    });
  });

  it('Найдет посылку из многоместного заказа, но она есть в отсканированных', () => {
    set.add('333~3_1');
    processBarcodes(
      ['333', '3_1'],
      ordersMap,
      set,
      onErrorHandler,
      onSuccesHandler,
    );

    expect(scanningProcessCache.get('error')).toEqual({
      withErrorDisplayMode: false,
      message: ALREADY_SCANNED_ERROR_MESSAGE,
    });
  });

  it('Найдет посылку из многоместного заказа по коду посылки', () => {
    processBarcodes(['3_1'], ordersMap, set, onErrorHandler, onSuccesHandler);

    expect(scanningProcessCache.get('order')).toEqual({
      placeCode: '3_1',
      order: ordersMap.get('333'),
    });
  });
});
