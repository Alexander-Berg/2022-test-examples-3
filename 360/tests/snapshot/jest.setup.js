import sinon from 'sinon';
import Adapter from 'enzyme-adapter-react-16';
import { configure, shallow } from 'enzyme';

configure({ adapter: new Adapter() });

beforeEach(() => {
  global.sinon = sinon.createSandbox();
});

afterEach(() => {
  if (global.sinon) {
    global.sinon.restore();
  }
});

global.shallow = shallow;
