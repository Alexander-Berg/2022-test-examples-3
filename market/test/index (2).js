const Generator = require('yeoman-generator');
const { getNames } = require('../../utils');

module.exports = class extends Generator {
    constructor(args, opts) {
        super(args, opts);

        this.appname = opts.appname;
        this.mode = opts.genMode;
    }

    write() {
        const name = this.appname;

        if (!name) {
            this.log('No can do with the empty name!');
            return;
        }

        const { packageName, className, entityName } = getNames(name);

        this.log(`Creating tests...`);

        if (this.mode === 'kotlin' || this.mode === 'both') {
            this.fs.copyTpl(
                this.templatePath('ControllerGetTest.kt.ejs'),
                this.destinationPath(
                    `${packageName}/src/test/kotlin/ru/yandex/market/wms/${packageName}/controller/${className}ControllerGetTest.kt`
                ),
                {
                    packageName,
                    className,
                    entityName,
                }
            );
            this.fs.copyTpl(
                this.templatePath('HttpAssert.kt.ejs'),
                this.destinationPath(
                    `${packageName}/src/test/kotlin/ru/yandex/market/wms/${packageName}/HttpAssert.kt`
                ),
                {
                    packageName,
                }
            );
        }

        // if (this.mode === 'java' || this.mode === 'both') {
        //     this.fs.copyTpl(
        //         this.templatePath('Model.java.ejs'),
        //         this.destinationPath(
        //             `${packageName}/src/main/java/ru/yandex/market/wms/${packageName}/model/${className}Model.java`
        //         ),
        //         {
        //             packageName,
        //             className,
        //             entityName,
        //         }
        //     );
        // }
    }
};
