import {IMetrika, DEFAULT_COMMON_PARAMS} from 'core-legacy'

export default class MetrikaStub implements IMetrika {
  generalReachGoalParams = DEFAULT_COMMON_PARAMS
  async load() {}
  async setParams() {}
  reachGoal() {}
  async clearCommonParams() {}
  async setCommonParams() {}
}
