import { BinaryOperation } from './binary-operation';

export type AvailableType = number | string | boolean | undefined;

export interface Expression {
  execute(values: Map<string, AvailableType>): AvailableType
}

export class NumVertex implements Expression {

  constructor(private value: number) {
  }

  execute(values: Map<string, AvailableType>): number {
    return this.value;
  }
}

export class StrVertex implements Expression {

  constructor(private value: string) {
  }

  execute(values: Map<string, AvailableType>): string {
    return this.value;
  }
}

export class VarVertex implements Expression {

  constructor(private name: string) {
  }

  execute(values: Map<string, AvailableType>): AvailableType {
    return values.get(this.name);
  }
}

export class NotVertex implements Expression {

  constructor(private expr: Expression) {
    //Priority.get().addOperation(this, NotVertex.priority);
  }
  static priority = 2;
  static symbol = "!";

  execute(values: Map<string, AvailableType>): AvailableType {
    return !this.expr.execute(values);
  }

  getSymbol(): string {
    return NotVertex.symbol;
  }

  getPriority(): number {
    return NotVertex.priority;
  }
}

export class OpVertex implements Expression {

  constructor(private expr1: Expression, private expr2: Expression, private op: BinaryOperation) {
  }

  execute(values: Map<string, AvailableType>): AvailableType {
    const leftValue = this.expr1.execute(values);
    const rightValue = this.expr2.execute(values);
    return this.op.execute(leftValue, rightValue);
  }
}