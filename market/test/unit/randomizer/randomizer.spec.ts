import fs from 'fs';
import path from 'path';
import Randomizer from '../../../src/randomizer';

function getFile(filename: string): string {
    return fs.readFileSync(path.resolve(__dirname, 'files', filename), 'utf-8');
}

describe('randomizer', () => {
    it('should extract classnames', () => {
        const html = getFile('classnames.html');

        const rand = new Randomizer(html, 13, undefined, 'm');
        expect(Object.keys(rand.randomNames)).toEqual([
            'class__name-test2',
            'class__name-test',
            'class-name',
            'classname',
            'endclass',
            'sn-test',
            'test3',
        ]);

        Object.values(rand.randomNames).forEach((randomValue) => {
            expect(randomValue).toMatch(/^m[a-z0-9]{12}$/);
        });
    });

    it('should extract id', () => {
        const html = getFile('id-and-classname.html');

        const rand = new Randomizer(html, 13, undefined, 'm');
        expect(Object.keys(rand.randomNames)).toEqual([
            'class__name-test2',
            'class__name-test',
            'my_cool_id-2',
            'class-name',
            'my-cool_id',
            'classname',
            'endclass',
            'sn-test',
            'test3',
        ]);

        Object.values(rand.randomNames).forEach((randomValue) => {
            expect(randomValue).toMatch(/^m[a-z0-9]{12}$/);
        });
    });

    it('should extract elements only from class or id attribute', () => {
        const html = getFile('add-attributes.html');

        const rand = new Randomizer(html, 13, undefined, 'm');
        expect(Object.keys(rand.randomNames)).toEqual([
            'class__name-test2',
            'class__name-test',
            'my_cool_id-2',
            'class-name',
            'my-cool_id',
            'classname',
            'endclass',
            'overflow',
            'sn-test',
            'test3',
        ]);

        Object.values(rand.randomNames).forEach((randomValue) => {
            expect(randomValue).toMatch(/^m[a-z0-9]{12}$/);
        });
    });

    it('should replace strings only in classes or ids', () => {
        const html = getFile('replace-only-classes-and-id.html');

        const rand = new Randomizer(html);

        rand.randomNames = Object.keys(rand.randomNames).reduce((res, randomKey) => {
            res[randomKey] = `rp-${randomKey}`;
            return res;
        }, {} as Record<string, string>);

        const result = rand.randomize(html);
        expect(result).toEqual(getFile('expected-replace-only-classes-and-id.html'));
    });

    it('should replace classnames and ids in css', () => {
        const html = getFile('add-attributes.html');
        const css = getFile('style.css');

        const rand = new Randomizer(html);

        rand.randomNames = Object.keys(rand.randomNames).reduce((res, randomKey) => {
            res[randomKey] = `rp-${randomKey}`;
            return res;
        }, {} as Record<string, string>);

        const result = rand.randomize(css);
        expect(result).toEqual(getFile('expected-style.css'));
    });

    it('shound not replace keywords are equals to class in css', () => {
        const html = getFile('add-attributes.html');
        const css = getFile('style-with-keywords.css');

        const rand = new Randomizer(html);

        rand.randomNames = Object.keys(rand.randomNames).reduce((res, randomKey) => {
            res[randomKey] = `rp-${randomKey}`;
            return res;
        }, {} as Record<string, string>);

        const result = rand.randomize(css);
        expect(result).toEqual(getFile('expected-style-with-keywords.css'));
    });

    it('shound not replace attributes are equals to class in css', () => {
        const html = getFile('add-attributes.html');
        const css = getFile('style-with-attr.css');

        const rand = new Randomizer(html);

        rand.randomNames = Object.keys(rand.randomNames).reduce((res, randomKey) => {
            res[randomKey] = `rp-${randomKey}`;
            return res;
        }, {} as Record<string, string>);

        const result = rand.randomize(css);
        expect(result).toEqual(getFile('expected-style-with-attr.css'));
    });

    it('should randomize selectors', () => {
        const html = getFile('add-attributes.html');

        const rand = new Randomizer(html);

        rand.randomNames = Object.keys(rand.randomNames).reduce((res, randomKey) => {
            res[randomKey] = `rp-${randomKey}`;
            return res;
        }, {} as Record<string, string>);

        const randomSelector = rand.randomize('#my_cool_id-2.endclass');
        expect(randomSelector).toEqual('#rp-my_cool_id-2.rp-endclass');
    });

    it('should use right random length', () => {
        const html = getFile('add-attributes.html');

        const rand = new Randomizer(html, 9, undefined, 'm');
        expect(Object.keys(rand.randomNames)).toEqual([
            'class__name-test2',
            'class__name-test',
            'my_cool_id-2',
            'class-name',
            'my-cool_id',
            'classname',
            'endclass',
            'overflow',
            'sn-test',
            'test3',
        ]);

        Object.values(rand.randomNames).forEach((randomValue) => {
            expect(randomValue).toMatch(/^m[a-z0-9]{8}$/);
        });
    });

    describe('components', () => {
        let randomizer: Randomizer;
        let generateRandomItem: jest.SpyInstance;

        beforeEach(() => {
            randomizer = new Randomizer(undefined, 13);
            generateRandomItem = jest.spyOn(Randomizer.prototype as any, 'generateRandomItem')
                .mockImplementation((name) => `rp-${name}`);
        });

        afterEach(() => {
            generateRandomItem.mockRestore();
        });

        it('.randomizeClass should randomize one class', () => {
            expect(randomizer.randomizeClass('test')).toEqual('rp-test');
        });

        it('.randomizeComponentStyle should randomize all classes and ids', () => {
            const css = getFile('components/style.css');
            const cssWithAttr = getFile('components/style-with-attr.css');
            const cssWithKeywords = getFile('components/style-with-keywords.css');

            expect(randomizer.randomizeComponentStyle(css)).toEqual(getFile('components/expected-style.css'));
            expect(randomizer.randomizeComponentStyle(cssWithAttr)).toEqual(
                getFile('components/expected-style-with-attr.css'),
            );
            expect(randomizer.randomizeComponentStyle(cssWithKeywords)).toEqual(
                getFile('components/expected-style-with-keywords.css'),
            );
        });

        it('.deepRandomizeNode should replace strings only in classes or ids', () => {
            const html = getFile('replace-only-classes-and-id.html');
            const div = document.createElement('div');
            div.innerHTML = html;

            const result = randomizer.deepRandomizeNode(div).innerHTML;

            expect(result).toEqual(getFile('expected-replace-only-classes-and-id.html'));
        });

        it('.randomizeString should randomize selector', () => {
            const html = getFile('replace-only-classes-and-id.html');
            const div = document.createElement('div');
            div.innerHTML = html;

            randomizer.deepRandomizeNode(div);

            expect(randomizer.randomizeString('div#my-cool_id.class__name-test .class__name-test2')).toEqual(
                'div#rp-my-cool_id.rp-class__name-test .rp-class__name-test2',
            );
        });

        it('should not randomize a class from library', () => {
            const html = getFile('replace-only-classes-and-id-with-blacklist.html');
            const div = document.createElement('div');
            div.innerHTML = html;

            randomizer.addDoNotRandomizePattern(/odometer/);

            const result = randomizer.deepRandomizeNode(div).innerHTML;

            expect(result).toEqual(getFile('expected-replace-only-classes-and-id-with-blacklist.html'));
        });

        it('should randomize a selector with a class from library', () => {
            const html = getFile('replace-only-classes-and-id-with-blacklist.html');
            const div = document.createElement('div');
            div.innerHTML = html;

            randomizer.addDoNotRandomizePattern(/odometer/);

            randomizer.deepRandomizeNode(div);

            expect(randomizer.randomizeString('div#my-cool_id.class__name-test .odometer')).toEqual(
                'div#rp-my-cool_id.rp-class__name-test .odometer',
            );
        });
    });
});
