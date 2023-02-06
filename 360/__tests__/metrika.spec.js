import {aggregate} from 'utils/metrika';
import * as session from 'configs/session';

describe('metrika', () => {
  describe('aggregate', () => {
    test('должен добавлять версию пакета статики', () => {
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate([]);

      expect(result).toEqual({version});
    });

    test('должен агрегировать простой путь', () => {
      const paths = [['1', '2', '3']];
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate(paths);

      expect(result).toEqual({
        '1': {'2': '3'},
        version
      });
    });

    test('должен агрегировать несколько независимых путей', () => {
      const paths = [['1', '2', '3'], ['2', '3', '4']];
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate(paths);

      expect(result).toEqual({
        '1': {'2': '3'},
        '2': {'3': '4'},
        version
      });
    });

    test('должен агрегировать несколько пересекающихся путей', () => {
      const paths = [['1', '2', '3'], ['1', '4', '5']];
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate(paths);

      expect(result).toEqual({
        '1': {
          '2': '3',
          '4': '5'
        },
        version
      });
    });

    test('должен агрегировать несколько одинаковых листьев в массив', () => {
      const paths = [['1', '2', '3'], ['1', '2', '3'], ['1', '2', '3']];
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate(paths);

      expect(result).toEqual({
        '1': {
          '2': ['3', '3', '3']
        },
        version
      });
    });

    test('должен агрегировать несколько одинаковых ветвей в массив', () => {
      const paths = [['1', '2', '3'], ['1', '2', '3'], ['1', '2', '3', '4']];
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate(paths);

      expect(result).toEqual({
        '1': {
          '2': ['3', '3', {'3': '4'}]
        },
        version
      });
    });

    test('должен агрегировать несколько одинаковых числовых листьев в сумму', () => {
      const paths = [['1', '2', 3], ['1', '2', 3], ['1', '2', 3]];
      const version = '5.6.7';

      sinon.stub(session, 'version').value(version);

      const result = aggregate(paths);

      expect(result).toEqual({
        '1': {
          '2': 9
        },
        version
      });
    });
  });
});
