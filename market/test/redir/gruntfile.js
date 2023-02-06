'use strict';

const path = require('path');

module.exports = function(grunt) {
    const branch = grunt.option('branch') || 'master';
    const host = grunt.option('host');

    grunt.option('config', host);

    const username =
        host && host.indexOf('sovetnik-dev-node') === 0 ? 'sovetnik' : 'robot-sovetnik';
    const pathToProject = `/home/${username}/sovetnik-redir`;

    let sshConfig = {};
    sshConfig[host] = {
        host: host + '.haze.yandex.net',
        username,
        agent: process.env.SSH_AUTH_SOCK,
        privateKey: grunt.file.read(
            path.join(
                process.env.HOME || process.env.HOMEPATH || process.env.USERPROFILE,
                '.ssh',
                'id_rsa',
            ),
        ),
    };

    grunt.initConfig({
        dch: {
            settings: {
                user: {
                    name: 'teamcity',
                    email: 'teamcity@yandex-team.ru',
                },
                project: 'yandex-' + grunt.file.readJSON('package.json').name,
            },
        },
        sshconfig: sshConfig,

        sshexec: {
            deploy: {
                command: [
                    `cd ${pathToProject} && ` +
                        'git fetch && ' +
                        'git checkout ' +
                        branch +
                        ' && ' +
                        'git pull && ' +
                        'npm install',
                    `cd ${pathToProject} && forever stop server.js || :`,
                    `cd ${pathToProject} && forever start server.js`,
                ],
            },
        },
    });

    grunt.loadNpmTasks('grunt-ssh');

    grunt.registerTask('deploy', ['sshexec:deploy']);
};
