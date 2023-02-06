import {PageFactory} from '../../lib/page';
import {View} from './view';
import {PageAtom} from './atom';

const loadData = () => Promise.resolve();

export const TestPage = new PageFactory(View, loadData, PageAtom);
