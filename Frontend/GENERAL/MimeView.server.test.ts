import type { ISerpDocument } from '@typings';
import { assert } from 'chai';
import { AdapterMimeView } from './MimeView.server';
import type { IMimeViewSnippet, IMimeViewButton } from './MimeView.typings';

describe('MimeView', function() {
    let adapter: AdapterMimeView;

    beforeEach(function() {
        adapter = Object.create(AdapterMimeView.prototype);
        // @ts-ignore
        adapter.context = {
            reportData: {
                isSearchApp: false,
            },
            log: { node: () => ({}) as object },
        };
        adapter.snippet = {} as IMimeViewSnippet;
        adapter.document = {
            mime: 'pdf',
            url: 'http://url/',
        } as ISerpDocument;
    });

    it('should include view disk button ', function() {
        adapter.context.reportData.isSearchApp = false;
        // @ts-ignore
        adapter.context.tld = 'ru';

        const buttons = adapter.transform()?.actions?.items;
        const saveButton = adapter.getDiskViewButton(adapter.document.mime as string, adapter.document.url);

        assert.deepInclude(buttons, saveButton);
    });

    it('should not include disk button in SearchApp', function() {
        adapter.context.reportData.isSearchApp = true;

        assert.notDeepInclude(
            adapter.transform()?.actions?.items as IMimeViewButton[],
            adapter.getDiskButtons(),
        );
    });

    it('should not include disk button at all domains except ru', function() {
        // @ts-ignore
        adapter.context.tld = 'com.tr';

        assert.notDeepInclude(
            adapter.transform()?.actions?.items as IMimeViewButton[],
            adapter.getDiskButtons(),
        );
    });

    it('should return null if doc.mime doesnt include allowedTypes ', function() {
        adapter.document.mime = 'test';

        assert.isNull(adapter.transform());
    });
});
