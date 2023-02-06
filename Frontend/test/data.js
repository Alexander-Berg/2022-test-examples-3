const zero = String.fromCharCode(0);

module.exports.withoutRemainder = [
    [3087549396, '1234', 1],
    [2075521194, '1234', 12],
    [1604520305, '1234', 123],

    [1516240278, '12345678', 1],
    [2529422961, '12345678', 12],
    [3839125429, '12345678', 123],

    [4274433957, '1234567890' + zero + zero, 1],
    [2956301241, '1234567890' + zero + zero, 12],
    [3511590003, '1234567890' + zero + zero, 123],
];

module.exports.withRemainder = [
    [2724904126, '1', 1],
    [3155455817, '1', 12],
    [3089878719, '1', 123],

    [2230443332, '12', 1],
    [3389454137, '12', 12],
    [3265283232, '12', 123],

    [301691705, '123', 1],
    [2304645262, '123', 12],
    [1678347533, '123', 123],

    [979258592, '12345', 1],
    [556557127, '12345', 12],
    [3985629747, '12345', 123],

    [1781600629, '123456', 1],
    [2090555644, '123456', 12],
    [1322204796, '123456', 123],

    [1608223741, '1234567', 1],
    [3869319178, '1234567', 12],
    [2273922404, '1234567', 123],

    [1516240278, '12345678', 1],
    [2529422961, '12345678', 12],
    [3839125429, '12345678', 123],

    [3588470886, '123456789', 1],
    [1525479827, '123456789', 12],
    [152176056, '123456789', 123],

    [148568240, '1234567890', 1],
    [3185380538, '1234567890', 12],
    [1318753088, '1234567890', 123],
];