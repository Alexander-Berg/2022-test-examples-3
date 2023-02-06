const path = require('path');

const gulp = require('gulp');
const concat = require('gulp-concat');

const { applyBorschikToFile } = require('../../helpers/borschik-api');
const borschik = require('../../gulp-plugins/borschik-include');
const concatByDeps = require('../../gulp-plugins/concat-by-deps');
const plainDeps = require('../../helpers/plain-deps');
const project = require('../../helpers/project-config');

const jsFilter = file => file.bem.tech === 'js';

module.exports.run = async ({
    allPath,
    depsPath,
    platform,
    pagePath
}) => {
    // Добавляем уровень страницы, где нет уровней переопределения
    await project.prepareCache();

    const deps = plainDeps.read(depsPath);
    const destDir = path.join(allPath, 'tests');

    await new Promise(resolve => {
        project.getFileStream(platform)
            .readableWith(`${pagePath}/blocks/**/*.js`)
            .pipe(concatByDeps(deps, jsFilter))
            .pipe(borschik.plugin())
            .pipe(concat('all.js'))
            .pipe(gulp.dest(destDir))
            .on('finish', async () => {
                await applyBorschikToFile(
                    path.join(destDir, 'all.js'),
                    'js',
                    {
                        minimize: false,
                        freeze: false,
                        techOptions: {
                            platform
                        }
                    }
                );

                resolve();
            });
    });
};
