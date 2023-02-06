import {persistStore} from 'redux-persist';
import localforage from 'localforage';

import device from 'device';
import formPersistor from 'utils/formPersistor';
import * as session from 'configs/session';

import serializationTransformer from '../serializationPersistTransformer';
import transformForm from '../form/transformer';
import {CommonPersistor} from '../commonPersistor';

jest.mock('redux-persist');
jest.mock('../serializationPersistTransformer', () => ({in: jest.fn()}));
jest.mock('../form/transformer', () => ({in: jest.fn()}));

const stubStorageName = 'commonStub';

describe('CommonPersistor', () => {
  beforeEach(() => {
    sinon.stub(session, 'storageName').value(stubStorageName);
  });

  test('должен правильно инициализировать опции при создании экземпляра', () => {
    const persistor = new CommonPersistor();

    expect(persistor.options.keyPrefix).toBe(`${stubStorageName}:common:`);
    expect(persistor.options.whitelist).toEqual(['form']);
    expect(persistor.options.serialise).toBe(false);
  });

  describe('#init', () => {
    beforeEach(() => {
      persistStore.mockImplementationOnce((store, options, cb) => cb());
      jest.spyOn(formPersistor, 'cancelPersist').mockImplementationOnce(() => {});
      jest
        .spyOn(localforage, 'createInstance')
        .mockImplementationOnce(() => ({localForageStorage: true}));
    });

    test('должен правильно инициализировать персист стора, если нет дополнительных флагов', () => {
      sinon.stub(device, 'allowPersistData').value(true);
      const persistor = new CommonPersistor();

      const cb = jest.fn();

      persistor.init({store: true}, cb);

      expect(persistor.options.whitelist).toEqual([
        'form',
        'settings',
        'layers',
        'todo',
        'offices',
        'timezones',
        'holidays',
        'migrations'
      ]);
      expect(persistor.options.storage).toEqual({localForageStorage: true});
      expect(persistor.options.store).toEqual({store: true});
      expect(cb).toHaveBeenCalledTimes(1);
      expect(formPersistor.cancelPersist).toHaveBeenCalledTimes(1);
    });

    test('не должен создавать коннект к хранилищу, если уже есть один коннект', () => {
      sinon.stub(device, 'allowPersistData').value(true);
      const persistor = new CommonPersistor();

      persistor.init({}, jest.fn());

      expect(localforage.createInstance).toHaveBeenCalledTimes(1);
      expect(persistor.options.storage).toEqual({localForageStorage: true});

      localforage.createInstance.mockClear();

      persistor.init({}, jest.fn());

      expect(localforage.createInstance).toHaveBeenCalledTimes(0);
    });

    test('должен добавить в белый список дополнительные части стора, если есть нужный флаг', () => {
      sinon.stub(device, 'allowPersistData').value(true);

      const persistor = new CommonPersistor();

      persistor.init({}, jest.fn());

      expect(persistor.options.whitelist).toEqual([
        'form',
        'settings',
        'layers',
        'todo',
        'offices',
        'timezones',
        'holidays',
        'migrations'
      ]);
    });
  });

  describe('#flushFormData', () => {
    beforeEach(() => {
      jest.spyOn(serializationTransformer, 'in').mockImplementation(() => {});
      jest.spyOn(transformForm, 'in').mockImplementation(() => {});
    });
    test('если нет коннекта к хранилищу, то не должен ничего делать', () => {
      const persistor = new CommonPersistor();

      persistor.options.store = {getState: jest.fn(() => ({form: {}}))};

      persistor.flushFormData();

      expect(serializationTransformer.in).toHaveBeenCalledTimes(0);
    });

    test('должен правильно записать данные в хранилище, если они есть в сторе', () => {
      const persistor = new CommonPersistor();

      persistor.options.store = {getState: jest.fn(() => ({form: {}}))};
      persistor.options.storage = {
        setItem: jest.fn()
      };

      const data = {serializedData: true};

      serializationTransformer.in.mockImplementationOnce(() => JSON.stringify(data));

      persistor.flushFormData();

      expect(persistor.options.storage.setItem).toHaveBeenCalledWith(
        `${persistor.options.keyPrefix}form`,
        JSON.stringify(JSON.stringify(data))
      );
    });

    test(
      'если данных нет в сторе, либо все данные были отфильтрованы при трансформации, ' +
        'то не должен ничего записывать в хранилище',
      () => {
        const persistor = new CommonPersistor();

        persistor.options.store = {getState: jest.fn(() => ({form: {}}))};
        persistor.options.storage = {
          setItem: jest.fn()
        };

        serializationTransformer.in.mockImplementationOnce(() => undefined);
        expect(persistor.options.storage.setItem).toHaveBeenCalledTimes(0);
      }
    );
  });
});
