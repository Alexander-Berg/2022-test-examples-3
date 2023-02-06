import {detectService} from '../utils/serviceUtils';
import expect from 'expect.js';

describe('Utils test', () => {
  it('must extract ico url', () => {
    const expected = {
      ico: 'conductor.ico',
      name: 'Conductor'
    };
    expect(detectService('https://c.yandex-team.ru/tickets/new')).to.eql(expected);
    expect(detectService('https://c.yandex-team.ru')).to.eql(expected);
    expect(detectService('https://c.yandex-team.ru/')).to.eql(expected);
  });

  it('must return emty icon url', () => {
    expect(detectService('https://ght.yandex-team.ru/tickets/new')).to.not.be.ok();
    expect(detectService('')).to.not.be.ok();
  });
});
