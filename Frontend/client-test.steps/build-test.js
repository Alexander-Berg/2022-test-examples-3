const gulp = require('gulp');
const concat = require('gulp-concat');
const filter = require('gulp-filter');
const project = require('../../helpers/project-config');
const comments = require('../../helpers/comments');
const wrapFile = require('../../gulp-plugins/wrap-file');
const readFile = require('../../gulp-plugins/read-file');

const testFilter = file => file.bem.tech === 'test' && !file.path.startsWith(project.rawConfig.paths.NODE_MODULES);

module.exports.run = async ({
    allPath,
    platform
}) => {
    // Добавляем уровень страницы, где нет уровней переопределения
    await project.prepareCache();

    return new Promise(resolve => {
        project.getFileStream(platform)
            .readable()
            .pipe(filter(testFilter))
            .pipe(readFile())
            .pipe(wrapFile({
                before: comments.beginSourceString,
                after: comments.endSourceString
            }))
            .pipe(concat('./tests/_test.js'))
            .pipe(gulp.dest(allPath))
            .on('finish', resolve);
    });
};
