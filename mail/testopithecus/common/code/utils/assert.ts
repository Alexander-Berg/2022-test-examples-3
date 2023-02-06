import { Int32 } from '../../ys/ys'

export function assertBooleanEquals(expected: boolean, actual: boolean, message: string): void {
  if (expected !== actual) {
    throw new Error(`${message}: expected=${expected}, but actual=${actual}`)
  }
}

export function assertInt32Equals(expected: Int32, actual: Int32, message: string): void {
  if (expected !== actual) {
    throw new Error(`${message}: expected=${expected}, but actual=${actual}`)
  }
}

export function assertStringEquals(expected: string, actual: string, message: string): void {
  if (expected !== actual) {
    throw new Error(`${message}: expected=${expected}, but actual=${actual}`)
  }
}
