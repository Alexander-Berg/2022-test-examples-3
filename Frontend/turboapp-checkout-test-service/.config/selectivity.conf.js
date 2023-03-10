module.exports = {
    root: '..',
    checks: {
        '*': [
            '!.svgo.yml',
            '!.eslintignore',
            '!.eslintrc.{js,json}',
            '!.stylelintignore',
            '!.stylelintrc.{js,json}',
            '.npmrc',
            '.nvmrc',
            'package-lock.json',
            'package.json',
            'tsconfig-server.json',
            'tsconfig.json',
            'webpack.config.js',
        ],
        deploy: [
            '.config/**',
            'public/**',
            'server/**',
            'src/**',
            'tools/**',
            '.dockerignore',
            'Dockerfile',
        ],
        e2e: [],
        unit: [],
        drone: [],
        hermione: [],
        'pulse:static': [],
        'pulse:shooter:touch': [],
        'pulse:shooter:desktop': [],
        'check:templates': [],
        'expflags:upload': [],
        'testpalm:validate': [],
    },
};
