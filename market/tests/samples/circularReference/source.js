declare type Type = {
    a: Type,
}

declare interface Interface {
    a: Interface;
}

declare class Class {
    a: Class;
}

opaque type B = {
    a: A,
};

type A = {
    Type: Type,
    Interface: Interface,
    Class: Class,
    a: A,
    b: B,
};

export {A};
