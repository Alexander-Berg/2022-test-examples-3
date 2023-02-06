type X = Error;

interface Y extends Error {
    y: Error;
}

class Z extends Error {
    z: Error;
}

export {X, Y, Z};
