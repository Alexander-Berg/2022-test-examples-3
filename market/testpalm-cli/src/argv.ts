import yargs from 'yargs'
import { mergeAll } from 'ramda'
import cosmiconfig from 'cosmiconfig'

import { version as VERSION, name as NAME } from '../package.json'
import {
  getOptions,
  checkOptions,
  defaults,
  ConfigOptions,
  AllOptions,
  AllOptionsKeys,
  Arguments,
} from './args'

const explorer = cosmiconfig(NAME /*, cosmiconfigOptions */)

const loadConfig = async (filepath: string): Promise<Partial<ConfigOptions>> =>
  explorer
    .load(filepath)
    .then(r => ((r && r.config) || {}) as Partial<ConfigOptions>)
    .catch(() => ({}))

type ArgvOptions<T extends AllOptionsKeys> = {
  usage: string
  // Доступные опции утилиты
  options?: T[]
  check?: typeof checkOptions
}

const buildArgv = <K extends AllOptionsKeys>({
  usage,
  options = [],
  check,
}: ArgvOptions<K>) => {
  const rawArgv = process.argv.slice(2)

  const argv = (yargs(rawArgv)
    .usage(usage)
    .version(VERSION)
    .options(getOptions(options))
    .alias('h', 'help').argv as unknown) as Arguments<
    Pick<AllOptions, K | 'config'>
  >

  // Проверяем отдельно, т.к. типизация иначе страдает
  let checkFn: typeof checkOptions = argv => checkOptions(argv)
  if (typeof check === 'function') {
    checkFn = argv => checkOptions(argv) && check(argv)
  }

  try {
    checkFn(argv)
  } catch (error) {
    console.log(error.message || error)
    process.exit(1)
  }

  return argv
}

export const parseArgv = async <T extends AllOptionsKeys>(
  argvOptions: ArgvOptions<T>,
) => {
  let { config: pathToConfig = defaults.config, ...argv } = buildArgv(
    argvOptions,
  )

  if (pathToConfig) {
    const config = await loadConfig(pathToConfig)
    argv = mergeAll([defaults, config, argv])
  }

  return argv
}
