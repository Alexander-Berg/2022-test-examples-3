import enzyme from 'enzyme';
import ReactAdapter from 'enzyme-adapter-react-16';

enzyme.configure({ adapter: new ReactAdapter() });
