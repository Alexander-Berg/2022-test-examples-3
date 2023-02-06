function saveDecorator({ target, decorator, args = [], property = '__class__' }) {
    target.__decorators__ = target.__decorators__ || {};
    target.__decorators__[property] = target.__decorators__[property] || {};
    target.__decorators__[property][decorator] = args;
}

export function classMemberDecorator(decorator) {
    return function(target, property, descriptor) {
        if (!property && !descriptor) {
            // если используется, как функция
            return target;
        }

        saveDecorator({ target, decorator, property });
        return descriptor;
    };
}

export function simpleClassDecorator(decorator) {
    return function(target) {
        saveDecorator({ target, decorator });
    };
}

export function classDecorator(decorator) {
    return function(...args) {
        return function(target) {
            saveDecorator({ target, decorator, args });
        };
    };
}
