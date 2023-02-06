import { Nullable } from '../ys/ys';
import { OperationCreator, EqOperationCreator } from './binary-operation';
import { Expression, NotVertex, NumVertex, OpVertex, StrVertex, VarVertex } from './expression';
import { Priority } from './priority';

export class ExpressionBuilder {

  constructor(private exprString: string) {
    const tmp = EqOperationCreator.get();
  }

  private position: number = 0;

  public static build(expression: string): Expression {
    const builder = new ExpressionBuilder(expression);
    return builder.make(0);
  }

  private make(priority: number): Expression {
    if (priority > Priority.get().lastPriority) {
      let expression: Expression;
      if (this.exprString.startsWith("(", this.position)) {
        this.skip("(");
        expression = this.make(0);
        this.skip(")");
      } else {
        expression = this.readSingle();
      }
      return expression;
    }

    const isNot = priority == 2 && this.exprString.startsWith("!", this.position);
    if (isNot) {
      this.skip("!");
    }
    let result = this.make(priority + 1);
    if (isNot) {
      result = new NotVertex(result);
    }

    let operation = this.readOperation(priority);
    while (operation !== null) {
      const operand = this.make(priority + 1);
      result = new OpVertex(result, operand, operation.create());
      operation = this.readOperation(priority);
    }

    return result;
  }

  private skip(prefix: string): void {
    if (this.exprString.startsWith(prefix, this.position)) {
      this.position += prefix.length;
    }
    while (this.position < this.exprString.length && this.exprString.charAt(this.position) == ' ') {
      this.position += 1;
    }
  }

  private readSingle(): Expression {
    const string1 = this.readString("'");
    if (string1 !== null) {
      return string1;
    }
    const string2 = this.readString("\"");
    if (string2 !== null) {
      return string2;
    }

    const startPosition = this.position;
    while (this.position < this.exprString.length && this.isLetterOrDigit(this.exprString.charAt(this.position))) {
      this.position += 1;
    }
    const token = this.exprString.substring(startPosition, this.position);
    this.skip(" ");

    if (!isNaN(Number(token))) {
      return new NumVertex(Number(token));
    }

    return new VarVertex(token);
  }

  private readString(quote: string): Nullable<Expression> {
    const startPosition = this.position;
    if (this.exprString.startsWith(quote, startPosition)) {
      this.position = this.exprString.indexOf(quote, this.position + 1);
      const expression = new StrVertex(this.exprString.substring(startPosition + 1, this.position));
      this.skip(quote);
      return expression;
    }
    return null;
  }

  private isLetterOrDigit(char: string): boolean {
    return char >= "0" && char <= "9" || char >= "a" && char <= "z" || char >= "A" && char <= "Z" || char == "_" || char == ".";
  }

  private readOperation(priority: number): Nullable<OperationCreator> {
    const operations = Priority.get().getOperationCreators(priority);
    for (const operation of operations) {
      if (this.exprString.startsWith(operation.getSymbol(), this.position)) {
        this.skip(operation.getSymbol());
        return operation;
      }
    }
    return null;
  }
}
