enum EPlatform {
  DESKTOP = 'desktop',
  PHONE = 'phone',
}

enum EFoo {
  BUZZ = 'buzz',
}

export type TFlagsDeclaration = [
  [
    `yxneo_${EPlatform}_my-awesome-feature`,
    {
      description: 'ОК',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
  [
    `yxneo_${EFoo}_my-awesome-feature`,
    {
      description: '!ОК - для перечисления "EFoo" не указана стратегия',
      values: '0|1',
      task: 'NERPA-XXXXX',
      type: boolean,
    },
  ],
];
