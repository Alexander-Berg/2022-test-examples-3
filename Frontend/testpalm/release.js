#!/usr/bin/env node

const { exec } = require('@yandex-int/frontend.ci.utils');
const { parse } = require('semver');
const packageJson = require('../../package.json');

const parsedVersion = parse(packageJson.version);
const project = 'chat';
const release = `${project}-v${parsedVersion.major}_${parsedVersion.minor}_${parsedVersion.patch}`;

function clone(src, dest) {
    exec(`npx @yandex-int/si.ci.testpalm-cli clone ${src} ${dest}`);
}

function sync(version) {
    exec(`npx @yandex-int/palmsync synchronize -p ${version}`);
}

clone(project, release);
sync(release);
