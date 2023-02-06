import { Registry } from './registry';

export interface Logger {
  log(message: string): void
}

export function log(message: string): void {
  const logger = Registry.get().logger;
  if (logger === null) {
    throw new Error('No logger found!')
  }
  logger.log(message)
}
