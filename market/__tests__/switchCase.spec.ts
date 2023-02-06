import switchCase from '../switchCase';

it('должен работать с простыми условиями', () => {
    const position = switchCase([
        [-1, 'Left'],
        [0, 'Middle'],
        [1, 'Right'],
    ]);

    expect(position(-1)).toBe('Left');
    expect(position(0)).toBe('Middle');
    expect(position(1)).toBe('Right');
    expect(position(2)).toBe(undefined);
    expect(position('0')).toBe(undefined);
});

it('должен работать с простыми условиями-предикатами', () => {
    const position = switchCase([
        [(n: number) => n < 0, 'Left'],
        [(n: number) => n === 0, 'Middle'],
        [(n: number) => n > 0, 'Right'],
    ]);

    expect(position(-1)).toBe('Left');
    expect(position(0)).toBe('Middle');
    expect(position(1)).toBe('Right');
    expect(position(2)).toBe('Right');
    expect(position('0')).toBe(undefined);
});

it('должен возвращать значение по-умолчанию, если никакие условия не подошли', () => {
    const position = switchCase(
        [
            [-1, 'Left'],
            [0, 'Middle'],
            [1, 'Right'],
        ],
        'Unknown',
    );

    expect(position(-1)).toBe('Left');
    expect(position(0)).toBe('Middle');
    expect(position(1)).toBe('Right');
    expect(position(2)).toBe('Unknown');
    expect(position('0')).toBe('Unknown');
});

it('должен работать со сложными условиями', () => {
    const side = switchCase(
        [
            [{ isLeft: false, isRight: false }, 'Neither'],
            [{ isLeft: true, isRight: false }, 'Left'],
            [{ isLeft: false, isRight: true }, 'Right'],
            [{ isLeft: true, isRight: true }, 'Both'],
        ],
        'Wrong',
    );

    expect(side({ isLeft: false, isRight: false })).toBe('Neither');
    expect(side({ isLeft: true, isRight: false })).toBe('Left');
    expect(side({ isLeft: false, isRight: true })).toBe('Right');
    expect(side({ isLeft: true, isRight: true })).toBe('Both');
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    expect(side({ isLeft: true, isRight: true, someOther: 'value' })).toBe('Both');
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    expect(side({ isLeft: true })).toBe('Wrong');
});

it('должен работать с неполным объектом условий', () => {
    const humanResources = switchCase(
        [
            [{ position: 'Lead', communicative: false }, 'Fire'],
            [{ position: 'Lead', communicative: true }, 'Hire'],
            [{ position: 'Middle' }, 'Hire'],
            [{ position: 'Junior' }, 'Hire'],
        ],
        'Wrong',
    );

    expect(humanResources({ position: 'Lead', communicative: false })).toBe('Fire');
    expect(humanResources({ position: 'Lead', communicative: true })).toBe('Hire');
    expect(humanResources({ position: 'Lead' })).toBe('Wrong');
    expect(humanResources({ position: 'Middle' })).toBe('Hire');
    expect(humanResources({ position: 'Middle', communicative: false })).toBe('Hire');
    expect(humanResources({ position: 'Middle', communicative: true })).toBe('Hire');
    expect(humanResources({ position: 'Junior' })).toBe('Hire');
    expect(humanResources({ position: 'Jun' })).toBe('Wrong');
});

it('должен работать с объектом условий, содержащим предикаты', () => {
    const isAdult = (age: number) => age >= 18;
    const isTeenager = (age: number) => age >= 14 && age < 18;
    const isChild = (age: number) => age < 14;
    const pupilType = switchCase(
        [
            [{ age: 6 }, 'Newbie'],
            [{ gender: 'Female', age: isChild }, 'Girl'],
            [{ gender: 'Male', age: isChild }, 'Boy'],
            [{ gender: 'Female', age: isTeenager }, 'Young lady'],
            [{ gender: 'Male', age: isTeenager }, 'Young man'],
            [{ gender: 'Female', age: isAdult }, 'Woman'],
            [{ gender: 'Male', age: isAdult }, 'Man'],
        ],
        'Wrong',
    );

    expect(pupilType({ gender: 'Male', age: 12 })).toBe('Boy');
    expect(pupilType({ age: 6 })).toBe('Newbie');
    expect(pupilType({ gender: 'Male', age: 6 })).toBe('Newbie');
    expect(pupilType({ gender: 'Female', age: 18 })).toBe('Woman');
    expect(pupilType({ age: 18 })).toBe('Wrong');
});
