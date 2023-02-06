import createResource from 'app/utils/createResource';
import defaultConfig from './config';
import api from './api';

export default createResource(defaultConfig, [api]);
