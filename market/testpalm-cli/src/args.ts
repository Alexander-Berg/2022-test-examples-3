import { pick } from 'ramda'
import { Options } from 'yargs'

import { name as NAME } from '../package.json'

type RecordBy<T, K> = { [P in keyof T]: K }

export type Arguments<T> = T & {
  /** Non-option arguments */
  _: string[]
  /** The script name or node command */
  $0: string
}

export type BaseOptions = {
  // ...
}

export type ConfigOptions = BaseOptions & Partial<{}>

// Positional
export type CliArgv = {
  args: string[]
}

export type CliConfigOption = {
  config: string
}

export type CliOptions = BaseOptions &
  CliConfigOption & {
    statface: string
    verbose: boolean
    command: string
    debug: boolean
  }

export type AllOptions = CliArgv & CliOptions & ConfigOptions

export const defaults = {
  config: `${NAME}.config.js`,
  // ...
}

// Не подствлявляем default тут, т.к. иначе они всегда буду перезатирать значения из конфига
export const options: RecordBy<CliOptions, Options> = {
  config: {
    alias: 'c',
    description: `The path to a ${NAME} config file.`,
    type: 'string',
  },
  statface: {
    description: 'Путь к отчету. Должен включать проект и путь внутри проекта.',
    alias: 's',
    type: 'string',
  },
  verbose: {
    alias: 'v',
    type: 'boolean',
  },
  command: {
    type: 'string',
  },
  debug: {
    type: 'boolean',
  },
}

export type AllOptionsKeys = keyof AllOptions

export const getOptions = (optionKeys: AllOptionsKeys[] | 'config') =>
  pick(['config', ...optionKeys], options)

export const checkOptions = <T>(argv: Arguments<T> & CliConfigOption): any => {
  if (argv.config && !argv.config.match(/\.js(on)?$/)) {
    throw new Error(
      'The --config option requires a file path with a .js or .json extension.\n' +
        `Example usage: ${NAME} --config ./${NAME}.config.js`,
    )
  }

  return true
}
