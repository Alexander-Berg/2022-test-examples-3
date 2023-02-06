import { Stack } from "../data-structures/stack";
import { MBTAction } from "../../mbt-abstractions";

export interface ActionLimitsStrategy {
  check(actions: Stack<MBTAction>): boolean
}
