import config, {assign} from '../config.js';

describe('config', function() {
  it('должен иметь правильное начальное значение', function() {
    expect(config).toEqual(window.Maya.config);
  });

  describe('assign', function() {
    it('должен правильно расширять конфиг', function() {
      assign({test: true});
      expect(config).toEqual(Object.assign({}, window.Maya.config, {test: true}));
    });
  });
});
