import LayersApi from '../LayersApi';

describe('LayersApi', () => {
  describe('getLayers', () => {
    test('должен отправлять запрос на получение слоев', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.getLayers();

      expect(api.post).toBeCalledWith('/get-user-layers');
    });
  });

  describe('getLayer', () => {
    test('должен отправлять запрос на получение слоя', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.getLayer('100');

      expect(api.post).toBeCalledWith('/get-layer', {id: '100'});
    });
  });

  describe('createToken', () => {
    test('должен отправлять запрос на создание токена', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.createToken('100', {forceNew: true});

      expect(api.post).toBeCalledWith('/do-obtain-layer-private-token', {
        id: '100',
        forceNew: true
      });
    });
  });

  describe('create', () => {
    test('должен отправлять запрос на создание слоя', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.create({
        name: 'new layer'
      });

      expect(api.post).toBeCalledWith('/do-create-layer', {
        name: 'new layer'
      });
    });
  });

  describe('update', () => {
    test('должен отправлять запрос на обновление слоя', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.update('100', {
        values: {
          name: 'new layer'
        },
        applyNotificationsToEvents: true
      });

      expect(api.post).toBeCalledWith('/do-update-layer', {
        id: '100',
        name: 'new layer',
        applyNotificationsToEvents: true
      });
    });
  });

  describe('delete', () => {
    test('должен отправлять запрос на удаление слоя', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.delete('100', '101');

      expect(api.post).toBeCalledWith('/do-delete-layer', {
        id: '100',
        recipientLayerId: '101'
      });
    });
  });

  describe('toggle', () => {
    test('должен отправлять запрос на переключение слоя', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.toggle('100', false);

      expect(api.post).toBeCalledWith('/do-toggle-layer', {
        id: '100',
        on: false
      });
    });
  });

  describe('share', () => {
    test('должен отправлять запрос на шаринг слоя', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.share('100', 'token');

      expect(api.post).toBeCalledWith('/do-share-layer', {
        id: '100',
        private_token: 'token'
      });
    });
  });

  describe('createFeed', () => {
    test('должен отправлять запрос на создание подписки', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.createFeed('url');

      expect(api.post).toBeCalledWith('/do-create-ics-feed', {url: 'url'});
    });
  });

  describe('updateFeed', () => {
    test('должен отправлять запрос на обновление подписки', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      layersApi.updateFeed('100');

      expect(api.post).toBeCalledWith('/do-force-feed-update', {id: '100'});
    });
  });

  describe('import', () => {
    test('должен отправлять запрос на импорт слоя из url', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      const data = {
        url: 'url',
        layer: {
          name: 'imported'
        }
      };

      layersApi.import(data);

      const formData = new FormData();

      formData.append('url', data.url);
      formData.append('layer', JSON.stringify(data.layer));

      expect(api.post).toBeCalledWith('/do-import-ics', formData);
    });

    test('должен отправлять запрос на импорт слоя из ics', () => {
      const api = {
        post: jest.fn()
      };
      const layersApi = new LayersApi(api);

      const data = {
        ics: ['ics'],
        layer: {
          name: 'imported'
        }
      };

      layersApi.import(data);

      const formData = new FormData();

      formData.append('ics', data.ics[0]);
      formData.append('layer', JSON.stringify(data.layer));

      expect(api.post).toBeCalledWith('/do-import-ics', formData);
    });
  });
});
