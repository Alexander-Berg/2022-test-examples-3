import { Int32 } from "../../../../ys/ys";

export class Stack<T>{
  stack: T[];

  constructor() {
    this.stack = []
  }

  push(item: T): void {
    this.stack.push(item);
  }

  pop(): void {
    this.stack.pop();
  }

  top(): T {
    return this.stack[this.stack.length - 1];
  }

  get(index: Int32): T {
    return this.stack[index];
  }

  clear(): void {
    this.stack = [];
  }

  size(): number {
    return this.stack.length;
  }
}
