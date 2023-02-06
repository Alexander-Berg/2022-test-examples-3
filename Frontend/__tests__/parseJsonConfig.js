const fs = require('mock-fs');
const parseJsonConfig = require('../parseJsonConfig');

describe('parseJsonConfig', () => {
    afterEach(() => {
        fs.restore();
    });
    it('should parse json files', () => {
        fs({
            'config.json': Buffer.from(`
      {
        "gzip": {
          "verbose": true
        }
      }
      `),
        });

        const obj = parseJsonConfig('config.json');
        expect(obj.gzip.verbose).toEqual(true);
    });
    it('should throw if json is not valid', () => {
        fs({
            'config.json': Buffer.from(`
      {
        gzip: {
          verbose: true
        }
      }
      `),
        });

        expect(() => parseJsonConfig('config.json')).toThrow();
    });
});
