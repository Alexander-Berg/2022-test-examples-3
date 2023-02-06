import { Int32 } from "../../../../ys/ys";
import { ActionLimitsStrategy } from "./action-limits-strategy";
import { MBTAction } from "../../mbt-abstractions";
import { Stack } from "../data-structures/stack";

export class TotalActionLimits implements ActionLimitsStrategy {
  constructor(private totalLimit: Int32) {
  }

  check(actions: Stack<MBTAction>): boolean {
    return this.totalLimit > actions.size();
  }
}
