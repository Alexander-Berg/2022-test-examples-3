jest.unmock('../src/Thread/messageprocess');

import toDomElement from '../src/Thread/toDomElement';
import { preprocessLinks, preprocessWmiCall } from '../src/Thread/messageprocess';

describe('#messageprocess', () => {
    it('preprocessWmiCall', () => {
        const input = toDomElement('<span class="wmi-callto">123</span>');
        expect(preprocessWmiCall(input).innerHTML).toEqual('<a href="tel:123">123</a>');
    });

    describe('#preprocessLinks', () => {
        var longLink = 'longlinklonglinklonglinklonglinklonglinklonglinklonglinklonglinklonglinklonglinklonglinklonglink';

        it('http protocol', () => {
            const input = toDomElement(`<a href="#">http://${longLink}</a>`);
            expect(preprocessLinks(input).innerHTML).toEqual('<a href="#">http://longlinklonglinklonglinklonglinklonglinklonglinklongl…</a>');
        });
        it('https protocol', () => {
            const input = toDomElement(`<a href="#">https://${longLink}</a>`);
            expect(preprocessLinks(input).innerHTML).toEqual('<a href="#">https://longlinklonglinklonglinklonglinklonglinklonglinklong…</a>');
        });
        it('unknown protocol', () => {
            const input = toDomElement(`<a href="#">${longLink}</a>`);
            expect(preprocessLinks(input).innerHTML).toEqual(`<a href="#">${longLink}</a>`);
        });
    });
});
