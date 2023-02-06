import _ from 'lodash';
import path from '../../../components/helpers/path';

describe('helperPath', () => {
    describe('split', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.jpg': ['disk', 'folder', 'path', 'to', 'parent', 'file.jpg'],
            '/disk/folder/path/to/parent/file.jpg///': ['disk', 'folder', 'path', 'to', 'parent', 'file.jpg'],
            '/disk/folder/path/to/parent////file.jpg': ['disk', 'folder', 'path', 'to', 'parent', 'file.jpg'],
            'disk////folder////path//to/parent/file.jpg': ['disk', 'folder', 'path', 'to', 'parent', 'file.jpg'],
            '/disk/parent/file/jpg/': ['disk', 'parent', 'file', 'jpg'],
            '////': [],
            '': []
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path.split(input);
                expect(result).toEqual(output);
            });
        });
    });

    describe('normalize', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.jpg': '/disk/folder/path/to/parent/file.jpg',
            '/disk/folder/path/to/parent/file.jpg///': '/disk/folder/path/to/parent/file.jpg',
            '/disk/folder/path/to/parent////file.jpg': '/disk/folder/path/to/parent/file.jpg',
            'disk////folder////path//to/parent/file.jpg': '/disk/folder/path/to/parent/file.jpg',
            '//disk////folder////path//to/parent/file.jpg': '/disk/folder/path/to/parent/file.jpg',
            '/disk/parent/file/jpg/': '/disk/parent/file/jpg',
            '22958453:disk/parent/file/': '/disk/parent/file',
            '23534543:/disk/parent///file/file.jpg': '/disk/parent/file/file.jpg',
            'disk/folder/2344555:mimimi.jpg': '/disk/folder/2344555:mimimi.jpg',
            'disk/folder2344555:/mimimi.jpg': '/disk/folder2344555:/mimimi.jpg',
            '////': '/',
            '': '/'
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path.normalize(input);
                expect(result).toBe(output);
            });
        });
    });

    describe('basename', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.jpg': 'file.jpg',
            '/disk/folder/path/to/parent/file.jpg///': 'file.jpg',
            '/disk/folder/path/to/parent////file.jpg': 'file.jpg',
            'disk////folder////path//to/parent/file.jpg': 'file.jpg',
            '/disk/parent/file/jpg/': 'jpg',
            '////': '',
            '': ''
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path.basename(input);
                expect(result).toBe(output);
            });
        });
    });

    describe('dirname', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.jpg': '/disk/folder/path/to/parent',
            '/disk/folder/path/to/parent/file.jpg///': '/disk/folder/path/to/parent',
            '/disk/folder/path/to/parent////file.jpg': '/disk/folder/path/to/parent',
            'disk////folder////path//to/parent/file.jpg': '/disk/folder/path/to/parent',
            '/public/OK7booQs1TJM6ugySfA1dNGoqcNVLyyUR02CdavJJb8=:/1.jpeg': '/public/OK7booQs1TJM6ugySfA1dNGoqcNVLyyUR02CdavJJb8=',
            '/disk/parent/file/jpg/': '/disk/parent/file',
            '////': null,
            '': null
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path.dirname(input);
                expect(result).toBe(output);
            });
        });
    });

    describe('extname', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.JPG': 'JPG',
            '/disk/folder/path/to/parent/file.jpg': 'jpg',
            '/disk/folder/path/to/parent/s.jpg///': 'jpg',
            '/disk/folder/path/to/parent////file': '',
            'file.jpg': 'jpg',
            '/disk/parent/file/jpg/': '',
            '/disk/parent/file/jpg/.': '',
            '////': '',
            '': ''
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path.extname(input);
                expect(result).toBe(output);
            });
        });
    });

    describe('extension with dot', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.JPG': '.JPG',
            '/disk/folder/path/to/parent/file.jpg': '.jpg',
            '/disk/folder/path/to/parent/s.jpg///': '.jpg',
            '/disk/folder/path/to/parent////file': '',
            'file.jpg': '.jpg',
            '/disk/parent/file/jpg/': '',
            '/disk/parent/file/jpg/.': '',
            '////': '',
            '': ''
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path.extname(input, true);
                expect(result).toBe(output);
            });
        });
    });

    it('addNameSuffix', () => {
        expect(path.addNameSuffix('file.ext', false, '')).toBe('file.ext');
        expect(path.addNameSuffix('file.ext', false, '_suffix')).toBe('file_suffix.ext');
        expect(path.addNameSuffix('file', false, '(1)')).toBe('file(1)');
        expect(path.addNameSuffix('dir', true, '(1)')).toBe('dir(1)');
        expect(path.addNameSuffix('dir.1/image', false, '(1)')).toBe('dir.1/image(1)');
        expect(path.addNameSuffix('dir.ext', true, '(1)')).toBe('dir.ext(1)');
        expect(path.addNameSuffix('dir.1/image.jpg', false, '(1)')).toBe('dir.1/image(1).jpg');
    });

    describe('join', () => {
        const tests = [{
            args: ['disk', 'trash', 'file.jpg'],
            output: '/disk/trash/file.jpg'
        }, {
            args: [['/disk//', '/trash//', '/file/jpg']],
            output: '/disk/trash/file/jpg'
        }, {
            args: [['/disk//', ['/trash//']], '/file/jpg'],
            output: '/disk/trash/file/jpg'
        }, {
            args: [],
            output: '/'
        }
        ];

        _.each(tests, (test) => {
            it('should return ' + test.output + ' ', function() {
                const result = path.join.apply(this, test.args);
                expect(result).toBe(test.output);
            });
        });
    });

    describe('isParent', () => {
        const tests = [
            {
                args: ['/files/folder', '/files/folder/test.txt'],
                output: true
            },
            {
                args: ['/files/folder', '/files/folder/subfolder/another'],
                output: true
            },
            {
                args: ['/files/folder-new', '/files/folder/test.txt'],
                output: false
            },
            {
                args: ['/files/folder', '/files/folder-new'],
                output: false
            },
            {
                args: ['/files/folder', '/files'],
                output: false
            },
            {
                args: ['/files/folder', '/files/folder'],
                output: false
            }
        ];

        _.each(tests, (test) => {
            it('should be ' + (test.output ? 'positive' : 'negative') + ' for ' + test.args.join(' > '), function() {
                const result = path.isParent.apply(this, test.args);
                expect(result).toBe(test.output);
            });
        });
    });

    describe('_replacePhotounlim', () => {
        const tests = {
            '/photounlim': '/photo',
            '/photounlim/path-to-file': '/photo/path-to-file',
            '/client/disk/photounlim/path-to-file': '/client/disk/photounlim/path-to-file'
        };

        _.each(tests, (output, input) => {
            it('should return ' + output + ' ', () => {
                const result = path._replacePhotounlim(input);
                expect(result).toEqual(output);
            });
        });
    });

    describe('revertFileExtension', () => {
        const tests = [
            {
                input: ['filename.txt', 'doc', 'txt'],
                output: 'filename.doc'
            },
            {
                input: ['filename', 'doc', ''],
                output: 'filename.doc'
            },
            {
                input: ['filename.TxT', 'doc', 'txt'],
                output: 'filename.doc'
            },
            {
                input: ['filename.doc', '', 'doc'],
                output: 'filename'
            },
            {
                input: ['Screenshot 2018-11-16 at 11.56.55', 'png', ''],
                output: 'Screenshot 2018-11-16 at 11.56.55.png'
            },
            {
                input: ['2_sorted.txtӾ', 'txt', 'txtӾ'],
                output: '2_sorted.txt'
            },
            {
                input: ['my.file.123.txt1', 'txt', 'txt1'],
                output: 'my.file.123.txt'
            }
        ];

        tests.forEach((test) => {
            it(`filename '${test.input[0]}' should be renamed back to '${test.output}'`, () => {
                expect(path.revertFileExtension.apply(null, test.input)).toBe(test.output);
            });
        });
    });
});
