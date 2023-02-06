import { AvailableType } from './expression';
import { Priority } from './priority';

export interface BinaryOperation {
  execute(expr1: AvailableType, expr2: AvailableType): AvailableType
}

export abstract class OperationCreator {
  protected priority = -1;
  protected symbol = "";

  public abstract create(): BinaryOperation;

  public getSymbol(): string {
    return this.symbol;
  }

  public getPriority(): number {
    return this.priority;
  }
}

export class EqOperationCreator extends OperationCreator {
  private static instance: EqOperationCreator = new EqOperationCreator();

  private constructor() {
    super();
    this.priority = 3;
    this.symbol = "==";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): EqOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new EqOperation();
  }
}

export class EqOperation implements BinaryOperation {
  execute(expr1: AvailableType, expr2: AvailableType): boolean | undefined {
    if (typeof expr1 == typeof expr2) {
      return expr1 == expr2;
    }
    return undefined;
  }
}

export class NotEqOperationCreator extends OperationCreator {
  private static instance: NotEqOperationCreator = new NotEqOperationCreator();

  private constructor() {
    super();
    this.priority = 3;
    this.symbol = "!=";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): NotEqOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new NotEqOperation();
  }
}

export class NotEqOperation implements BinaryOperation {
  execute(expr1: AvailableType, expr2: AvailableType): boolean | undefined {
    return !(new EqOperation()).execute(expr1, expr2);
  }
}

export class LessOperationCreator extends OperationCreator {
  private static instance: LessOperationCreator = new LessOperationCreator();

  private constructor() {
    super();
    this.priority = 3;
    this.symbol = "<";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): LessOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new LessOperation();
  }
}

export class LessOperation implements BinaryOperation {
  execute(expr1: number, expr2: number): boolean {
    return expr1 < expr2;
  }
}

export class GreaterOperationCreator extends OperationCreator {
  private static instance: GreaterOperationCreator = new GreaterOperationCreator();

  private constructor() {
    super();
    this.priority = 3;
    this.symbol = ">";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): GreaterOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new GreaterOperation();
  }
}

export class GreaterOperation implements BinaryOperation {
  execute(expr1: number, expr2: number): boolean {
    return expr1 > expr2;
  }
}

export class LessOrEqOperationCreator extends OperationCreator {
  private static instance: LessOrEqOperationCreator = new LessOrEqOperationCreator();

  private constructor() {
    super();
    this.priority = 3;
    this.symbol = "<=";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): LessOrEqOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new LessOrEqOperation();
  }
}

export class LessOrEqOperation implements BinaryOperation {
  execute(expr1: number, expr2: number): boolean {
    return expr1 <= expr2;
  }
}

export class GreaterOrEqOperationCreator extends OperationCreator {
  private static instance: GreaterOrEqOperationCreator = new GreaterOrEqOperationCreator();

  private constructor() {
    super();
    this.priority = 3;
    this.symbol = ">=";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): GreaterOrEqOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new GreaterOrEqOperation();
  }
}

export class GreaterOrEqOperation implements BinaryOperation {
  execute(expr1: number, expr2: number): boolean {
    return expr1 >= expr2;
  }
}

export class AndOperationCreator extends OperationCreator {
  private static instance: AndOperationCreator = new AndOperationCreator();

  private constructor() {
    super();
    this.priority = 1;
    this.symbol = "&&";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): AndOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new AndOperation();
  }
}

export class AndOperation implements BinaryOperation {
  execute(expr1: boolean | undefined, expr2: boolean | undefined): boolean | undefined {
    if (typeof expr1 == "boolean" && typeof expr2 == "boolean") {
      return expr1 && expr2;
    }
    return undefined;
  }
}

export class OrOperationCreator extends OperationCreator {
  private static instance: OrOperationCreator = new OrOperationCreator();

  private constructor() {
    super();
    this.priority = 0;
    this.symbol = "||";
    Priority.get().addOperationCreator(this, this.priority);
  }

  public static get(): OrOperationCreator {
    return this.instance;
  }

  public create(): BinaryOperation {
    return new OrOperation();
  }

}

export class OrOperation implements BinaryOperation {
  execute(expr1: boolean | undefined, expr2: boolean | undefined): boolean | undefined {
    if (typeof expr1 == "boolean" && typeof expr2 == "boolean") {
      return expr1 || expr2;
    }
    return undefined;
  }
}
