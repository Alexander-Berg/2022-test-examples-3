export interface TestamentPluginsConfig {
    inlineRequire?: {
        enabled: boolean;
        ignoredRequires: string[];
    };
}

const getTestamentBabelConfig = (pluginsConfig?: TestamentPluginsConfig) => {
    const config: any = {
        presets: [
            [
                '@babel/preset-env',
                {
                    targets: {
                        node: 12,
                    },
                    loose: true,
                },
            ],
            '@babel/react',
            '@babel/flow',
        ],
        plugins: [
            '@babel/plugin-proposal-class-properties',
            '@babel/plugin-proposal-private-methods',
            ['inline-react-svg', {svgo: false}],
            'react-css-modules',
            require.resolve('./plugins/replace-webpack.js'),
        ],
    };

    if (
        process.env.PLATFORM === 'desktop' ||
        process.env.PLATFORM === 'touch'
    ) {
        config.plugins.push('reselector/babel');
    }

    if (pluginsConfig?.inlineRequire?.enabled) {
        config.presets.unshift({
            plugins: [
                [
                    require.resolve('./plugins/inline-require'),
                    {
                        ignoredRequires:
                            pluginsConfig?.inlineRequire?.ignoredRequires ?? [],
                    },
                ],
            ],
        });
    }

    return config;
};

export {getTestamentBabelConfig};
