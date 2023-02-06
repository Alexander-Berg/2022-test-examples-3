const helperPath = require('helpers/path');
const helperEditorDoc = require('helpers/editor-doc');

describe('routerEdit', () => {
    const docPath = '/disk/Загрузки';
    const sk = 'yafb246a76c2c270ffb05e2ecd2262da9';
    const layoutName = 'edit';
    const action = 'editnew';

    describe('Метод `generateUrl`', () => {
        beforeEach(() => {
            ns.router.routes = {
                route: helperEditorDoc.route,
                rewriteParams: helperEditorDoc.rewriteParams
            };
            ns.page.currentUrl = '/';
            ns.router.init();
        });

        afterEach(() => {
            delete ns.router._routes;
            delete ns.router.routes;
        });

        it('для лейаута `' + layoutName + '` и клиента `disk` вернёт: ', () => {
            expect(ns.router.generateUrl(
                layoutName,
                { idClient: 'disk', idDoc: helperPath.escape(docPath + '/Таблица (90).xlsx'), sk }
            )).to.eql('/edit/disk%2Fdisk%2F%25D0%2597%25D0%25B0%25D0%25B3%25D1%2580%25D1%2583%25D0%25B7%25D0%25BA%25D0%25B8%2F%25D0%25A2%25D0%25B0%25D0%25B1%25D0%25BB%25D0%25B8%25D1%2586%25D0%25B0%2520(90).xlsx?sk=yafb246a76c2c270ffb05e2ecd2262da9');
        });

        it('для действия `' + action + '` и расширения `docx` вернёт: ', () => {
            expect(ns.router.generateUrl(
                layoutName,
                { action, ext: 'docx', idDoc: docPath, sk }
            )).to.eql('/editnew/docx%2Fdisk%2F%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8?sk=yafb246a76c2c270ffb05e2ecd2262da9');
        });

        it('для действия `' + action + '` и расширения `pptx` вернёт: ', () => {
            expect(ns.router.generateUrl(
                layoutName,
                { action, ext: 'pptx', idDoc: docPath, sk }
            )).to.eql('/editnew/pptx%2Fdisk%2F%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8?sk=yafb246a76c2c270ffb05e2ecd2262da9');
        });

        it('для действия `' + action + '` и расширения `xlsx` вернёт: ', () => {
            expect(ns.router.generateUrl(
                layoutName,
                { action, ext: 'xlsx', idDoc: docPath, sk }
            )).to.eql('/editnew/xlsx%2Fdisk%2F%D0%97%D0%B0%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B8?sk=yafb246a76c2c270ffb05e2ecd2262da9');
        });
    });
});
