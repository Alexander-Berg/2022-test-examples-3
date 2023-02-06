import {createResource} from '@yandex-market/b2b-core/app';
import defaultConfig from './config';
import api from './api';

export default createResource(defaultConfig, [api]);
