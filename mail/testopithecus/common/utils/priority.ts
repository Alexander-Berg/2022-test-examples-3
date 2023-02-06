import { max } from '../code/utils/utils';
import { OperationCreator } from './binary-operation';

export class Priority {
  private static instance: Priority = new Priority();

  private constructor() {
  }

  private operationCreators: Map<number, OperationCreator[]> = new Map<number, OperationCreator[]>();
  public lastPriority: number = -1;

  public static get(): Priority {
    return this.instance;
  }

  public addOperationCreator(creator: OperationCreator, priority: number): void {
    if (this.operationCreators.has(priority)) {
      this.operationCreators.get(priority)!.push(creator);
    } else {
      this.operationCreators.set(priority, [creator]);
    }
    this.lastPriority = max(this.lastPriority, priority);
  }

  public getOperationCreators(priority: number): OperationCreator[] {
    if (this.operationCreators.has(priority)) {
      this.operationCreators.get(priority)!.sort((c1, c2) => c2.getSymbol().length - c1.getSymbol().length);
      return this.operationCreators.get(priority)!;
    }
    return [];
  }
}
