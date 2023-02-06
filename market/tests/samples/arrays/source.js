type Type = {
    a: string[],
    b: Array<string>,
    c: $ReadOnlyArray<string>,
};

interface Interface extends Array<string> {
    a: string[];
    b: Array<string>;
    c: $ReadOnlyArray<string>;
}

class Class extends Array<string> {
    a: string[];
    b: Array<string>;
    c: $ReadOnlyArray<string>;
}

export {Type, Interface, Class};
