import {assocPath} from 'ramda';

import mockRoutes from 'configs/route';
import config from 'configs/development/node';
import {port} from 'configs/jest/testServer/config';

const mockConfig = port ? assocPath(['server', 'port'], port, config) : config;

jest.mock(
    'config',
    () => ({
        version: 'version.txt',
        routes: mockRoutes,
        config: mockConfig,
    }),
    {virtual: true},
);
