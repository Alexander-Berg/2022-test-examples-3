import { getWebpackDevServerConfig } from './webpack-dev-server.config';
import { getWebpackProdConfig } from './webpack.prod.config';
import { getWebpackConfig } from './webpack.config';

jest.mock('fs', () => ({
  readFileSync: () => 'certific',
  existsSync: () => true,
  writeFileSync: () => 1,
}));

jest.mock('webpack', () => ({
  HotModuleReplacementPlugin: function () {
    return {};
  },
}));

jest.mock('html-webpack-plugin', () => {
  return function () {
    return {};
  };
});

jest.mock('fork-ts-checker-webpack-plugin', () => {
  return function () {
    return {};
  };
});

describe('webpack', () => {
  it('getWebpackDevServerConfig', () => {
    console.error = () => 1;
    const devWebpackConfig = getWebpackDevServerConfig({ apiDist: [{ path: '/api', target: 'target_host' }] });
    expect(devWebpackConfig).toEqual({
      host: 'localhost.msup.yandex-team.ru',
      open: true,
      port: 8449,
      https: {
        key: 'certific',
        cert: 'certific',
      },
      proxy: {
        '/api': {
          changeOrigin: true,
          headers: {},
          secure: false,
          target: 'target_host',
        },
      },
      historyApiFallback: {
        disableDotRule: true,
      },
      client: {
        overlay: {
          errors: true,
          warnings: false,
        },
      },
    });
  });

  it('getWebpackProdConfig', () => {
    const prodConfig = getWebpackConfig(
      { webpack: conf => ({ ...conf, plugins: conf.plugins?.concat({} as any) }) },
      true
    );
    // Dotenv, forkTsChecker, HtmlWebpack, MiniCssExtractor and empty object from MboCoreConfig
    expect(prodConfig.plugins).toHaveLength(5);
  });
});
