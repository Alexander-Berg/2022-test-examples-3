const fs = require('fs');
const nodePlop = require('node-plop');
const fg = require('fast-glob');
const plop = nodePlop('./tools/generators/plopfile.js');
const { toPascalCase } = require('../utils');
const { commands } = require('../const');

const testGenerator = generator => {
    const basicAdd = plop.getGenerator(generator.command);

    try {
        if (generator.command === commands.GENERATE_ICON) {
            fg.sync([generator.targetPath + `Icon_type_${generator.options.name}.*`])
                .forEach(file => {
                    fs.unlinkSync(file);
                });
        } else if (generator.command === commands.GENERATE_EXP_FEATURE ||
            generator.command === commands.GENERATE_EXP_COMPONENT ||
            generator.command === commands.GENERATE_CSS_ONLY_EXP) {
            const pathExperiment = `${generator.targetPath}${generator.options.flagName}`;

            fs.rmdirSync(pathExperiment, { recursive: true });
        } else {
            fs.rmdirSync(generator.targetPath + toPascalCase(generator.options.name), { recursive: true });
        }
    } catch (e) {
        console.log('Ошибка при удалении директории');
        console.log(e);
    }

    basicAdd.runActions(generator.options)
        .then(function(results) {
            if (results.failures.length === 0) {
                console.log('Кодогенерация прошла успешно');
            } else {
                console.log('В некоторых шагах кодогенерации произошли ошибки!');
                console.log(results.failures);
            }
        }).catch(err => {
            console.error('Кодогенерация завершилась с ошибками:\n');
            console.log(err);
        });
};

module.exports = testGenerator;
