import getDocviewerSearch from '../../../../app/helpers/get-docviewer-search';

describe('app/helpers/get-docviewer-search', () => {
    it('неподдерживаемые файлы', () => {
        expect(getDocviewerSearch('hash', 'name.mp3', 'mp3', 'audio/mp3', true)).toBeUndefined();
        expect(getDocviewerSearch('hash', 'name.sketch', 'sketch', 'image/sketch', true)).toBeUndefined();
        expect(getDocviewerSearch('hash', 'name.avi', 'avi', 'video/что-то там', true)).toBeUndefined();
    });

    it('поддерживаемые по расширению', () => {
        expect(getDocviewerSearch('hash', 'name.pdf', 'pdf', '', true)).toEqual('?url=ya-disk-public%3A%2F%2Fhash&name=name.pdf');
        expect(getDocviewerSearch('hash/path-to-file', 'name.doc', 'doc', '', true)).toEqual('?url=ya-disk-public%3A%2F%2Fhash%2Fpath-to-file&name=name.doc');
        expect(getDocviewerSearch('hash', 'name.cab', 'cab')).toEqual('?url=ya-disk-public%3A%2F%2Fhash&name=name.cab');
    });

    it('поддерживаемые по медиатипу', () => {
        expect(getDocviewerSearch('hash', 'name.pdf2', 'pdf2', 'application/pdf', true)).toEqual('?url=ya-disk-public%3A%2F%2Fhash&name=name.pdf2');
        expect(getDocviewerSearch('hash/path-to-file', 'name', '', 'application/gzip')).toEqual('?url=ya-disk-public%3A%2F%2Fhash%2Fpath-to-file&name=name');
    });
});
