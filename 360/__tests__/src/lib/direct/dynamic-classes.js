let dynamicClasses;

describe('src/lib/direct/dynamic-classes', () => {
    beforeEach(() => {
        dynamicClasses = require('lib/direct/dynamic-classes');
        jest.spyOn(Math, 'random').mockReturnValue(0.1);
    });

    afterEach(() => jest.resetModules());

    describe('#getClassById', () => {
        it('should return class with random name', () => {
            expect(dynamicClasses.getClassById()).toEqual('ccccc');
        });

        it('should return same class with random name', () => {
            expect(dynamicClasses.getClassById()).toEqual('ccccc');
            Math.random.mockReturnValue(0.2);
            expect(dynamicClasses.getClassById()).toEqual('ccccc');
        });
    });

    describe('#createClassById', () => {
        beforeEach(() => {
            jest.spyOn(document, 'getElementById').mockImplementation();
            jest.spyOn(document, 'createElement').mockImplementation();
            jest.spyOn(document.head, 'appendChild').mockImplementation();
        });

        afterEach(() => jest.clearAllMocks());

        it('should create class', () => {
            const style = {};
            document.createElement.mockReturnValue(style);
            document.head.appendChild.mockReturnValue(style);

            expect(dynamicClasses.createClassById('test', 'width: 42;')).toEqual('ccccc');
            expect(style).toEqual({
                id: 'ccccc',
                innerHTML: '.ccccc { width: 42; }',
                type: 'text/css'
            });
            expect(document.getElementById).toHaveBeenCalledWith('ccccc');
            expect(document.createElement).toHaveBeenCalledWith('style');
            expect(document.head.appendChild).toHaveBeenCalledWith(style);
        });

        it('should update class', () => {
            const style = {};
            document.getElementById.mockReturnValue(style);
            document.head.appendChild.mockReturnValue(style);

            expect(dynamicClasses.createClassById('test', 'width: 42;')).toEqual('ccccc');
            expect(style).toEqual({
                id: 'ccccc',
                innerHTML: '.ccccc { width: 42; }',
                type: 'text/css'
            });
            expect(document.getElementById).toHaveBeenCalledWith('ccccc');
            expect(document.createElement).not.toHaveBeenCalled();
            expect(document.head.appendChild).toHaveBeenCalledWith(style);
        });
    });

    describe('#getRandomNames', () => {
        it('should generate random names', () => {
            const [topOrder, bottomOrder] = [[true], [false, true]];
            expect(dynamicClasses.getRandomNames(topOrder, bottomOrder)).toEqual({
                direct: 'ccccc',
                placeholder: 'ccccc',

                'top-direct': 'ccccc',
                'top-0': 'ccccc',

                'bottom-direct': 'ccccc',
                'bottom-0': 'ccccc',
                'bottom-1': 'ccccc'
            });
        });
    });
});
