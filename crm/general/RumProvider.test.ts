import { history } from 'entry/appHistory';
import rumProvider from './RumProvider';
import Rum from './__mocks__/Rum';

rumProvider.rumInstance = Rum;

const testParamName = 'paramName';
const testParamValue = 'paramValue';
const testParamObject = {
  [testParamName]: testParamValue,
};

describe('Rum-Service: RumProvider', () => {
  afterEach(() => {
    Rum._vars = {};
    rumProvider.globalAdditional = {};
  });

  describe('.listenHistory', () => {
    describe('when url is changing', () => {
      it('sets "-page" param', () => {
        history.push('testUrl');
        expect(rumProvider.getGlobalParam('-page')).toEqual('testUrl');
      });
    });
  });

  describe('.getGlobalParam', () => {
    it(`gets param by name`, () => {
      rumProvider.setGlobalParams(testParamObject);
      expect(rumProvider.getGlobalParam(testParamName)).toEqual(testParamValue);
    });
  });

  describe('.sendTimeMark', () => {
    it(`sends timeMark`, () => {
      const timeMarkName = 'timeMarkName';
      const date = Date.now();
      const mockAdditional = { '-additional': rumProvider.prepareAdditional({ global: {} }) };
      rumProvider.sendTimeMark(timeMarkName, date);
      expect(Rum.sendTimeMark).toBeCalledWith(timeMarkName, date, undefined, mockAdditional);
    });
  });

  describe('.makeSubPage', () => {
    it(`creates new subPage`, () => {
      const subPageName = 'someSubPageName';
      Rum.makeSubPage.mockReturnValueOnce(subPageName);
      const subPage = rumProvider.makeSubPage(subPageName);
      expect(subPage).toEqual(subPageName);
    });
  });

  describe('.setGlobalAdditional', () => {
    it(`sets additional`, () => {
      rumProvider.setGlobalAdditional(testParamObject);
      expect(rumProvider.getGlobalAdditional()).toEqual(testParamObject);
    });

    it(`merges old and new additional`, () => {
      rumProvider.setGlobalAdditional(testParamObject);
      rumProvider.setGlobalAdditional({ test1: 'someValue1' });
      expect(rumProvider.getGlobalAdditional()).toEqual({
        ...testParamObject,
        test1: 'someValue1',
      });
    });

    describe('when incorrect additional', () => {
      it('throws error', () => {
        expect(() => {
          // @ts-ignore
          rumProvider.setGlobalAdditional(234);
        }).toThrow('incorrect type of additional');
      });
    });
  });

  describe('.setGlobalParams', () => {
    describe('when incorrect params', () => {
      it('throws error', () => {
        expect(() => {
          // @ts-ignore
          rumProvider.setGlobalParams([1, 2, 3]);
        }).toThrow('incorrect type of params');
      });
    });
  });
});
