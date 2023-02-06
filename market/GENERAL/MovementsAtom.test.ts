import { Store } from '@reatom/core';

import { SupplierOffer } from 'src/java/definitions';
import { configureStore } from 'src/models/store';
import { MappingsAtom, SetMappingsAction } from './MappingsAtom';
import { HasMovementsAtom, MappingMovementsActions, MappingMovementsAtom, OfferMovementsAtom } from './MovementsAtom';
import { MappingsSelectionActions, MappingsSelectionAtom } from './MappingsSelectionAtom';
import { setupTestStore } from 'src/test/setupTestStore';

describe('MappingMovementsAtom::', () => {
  let store: Store;

  beforeEach(() => {
    const { api, history } = setupTestStore();
    store = configureStore({ dependencies: { api, history } });
  });

  it('inits', () => {
    expect(store.getState(MappingMovementsAtom)).toEqual({});
  });
  it('setAll works', () => {
    store.subscribe(MappingMovementsAtom, jest.fn());
    const testMovements = { 123: [123] };
    store.dispatch(MappingMovementsActions.setAll(testMovements));
    expect(store.getState(MappingMovementsAtom)).toEqual(testMovements);
  });
  it('set works', () => {
    store.subscribe(MappingMovementsAtom, jest.fn());
    store.dispatch(MappingMovementsActions.set({ entityId: 123, mappingIds: [234] }));
    expect(store.getState(MappingMovementsAtom)).toEqual({ 123: [234] });
  });
  it('move works', () => {
    store.subscribe(MappingsAtom, jest.fn());
    store.subscribe(MappingMovementsAtom, jest.fn());
    store.subscribe(MappingsSelectionAtom, jest.fn());
    store.dispatch(SetMappingsAction(getOffers([123, 234, 345, 321, 432, 534, 131, 424, 524])));
    store.dispatch(
      MappingMovementsActions.setAll({ 123: [123, 234, 345], 234: [321, 432, 534], 345: [131, 424, 524] })
    );
    store.dispatch(MappingsSelectionActions.set({ 234: true, 432: true, 524: true, 123: false }));
    store.dispatch(MappingMovementsActions.move({ entityId: 345 }));
    expect(store.getState(MappingMovementsAtom)).toEqual({
      123: [123, 345],
      234: [321, 534],
      345: [131, 424, 234, 432, 524],
    });
    store.dispatch(MappingsSelectionActions.set({ 234: true }));
    store.dispatch(MappingMovementsActions.move({ entityId: 543 }));
    expect(store.getState(MappingMovementsAtom)).toEqual({
      123: [123, 345],
      234: [321, 534],
      345: [131, 424, 432, 524],
      543: [234],
    });
  });
  it('move removeOnly works', () => {
    store.subscribe(MappingsAtom, jest.fn());
    store.subscribe(MappingMovementsAtom, jest.fn());
    store.subscribe(MappingsSelectionAtom, jest.fn());
    store.dispatch(SetMappingsAction(getOffers([123, 234, 345, 321, 432, 534, 131, 424, 524])));
    store.dispatch(
      MappingMovementsActions.setAll({ 123: [123, 234, 345], 234: [321, 432, 534], 345: [131, 424, 524] })
    );
    store.dispatch(MappingsSelectionActions.set({ 234: true, 432: true, 524: true, 123: false }));
    store.dispatch(MappingMovementsActions.move({ entityId: 543, removeOnly: true }));
    expect(store.getState(MappingMovementsAtom)).toEqual({
      123: [123, 345],
      234: [321, 534],
      345: [131, 424],
    });
  });
  it('remove works', () => {
    store.subscribe(MappingMovementsAtom, jest.fn());
    store.dispatch(
      MappingMovementsActions.setAll({ 123: [123, 234, 345], 234: [321, 432, 534], 345: [131, 424, 524] })
    );
    store.dispatch(MappingMovementsActions.remove(345));
    expect(store.getState(MappingMovementsAtom)).toEqual({
      123: [123, 234, 345],
      234: [321, 432, 534],
      345: [],
    });
  });
  it('reset works', () => {
    store.subscribe(MappingMovementsAtom, jest.fn());
    store.dispatch(
      MappingMovementsActions.setAll({ 123: [123, 234, 345], 234: [321, 432, 534], 345: [131, 424, 524] })
    );
    store.dispatch(MappingMovementsActions.reset());
    expect(store.getState(MappingMovementsAtom)).toEqual({});
  });
  it('hasMovementsAtom works', () => {
    store.subscribe(MappingMovementsAtom, jest.fn());
    expect(store.getState(HasMovementsAtom)).toEqual(false);
    store.dispatch(MappingMovementsActions.setAll({ 123: [123, 234, 345] }));
    expect(store.getState(HasMovementsAtom)).toEqual(true);
    store.dispatch(MappingMovementsActions.setAll({ 123: [] }));
    expect(store.getState(HasMovementsAtom)).toEqual(false);
  });
  it('OfferMovementsAtom works', () => {
    store.subscribe(MappingMovementsAtom, jest.fn());
    store.dispatch(MappingMovementsActions.setAll({ 123: [123, 234, 345], 234: [321, 432, 534] }));
    expect(store.getState(OfferMovementsAtom)).toEqual({
      123: 123,
      234: 123,
      345: 123,
      321: 234,
      432: 234,
      534: 234,
    });
  });
});

function getOfferMock(data: Partial<SupplierOffer>) {
  return data as SupplierOffer;
}

function getOffers(ids: number[]) {
  return ids.map(id =>
    getOfferMock({
      internalId: id,
    })
  );
}
