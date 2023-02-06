import type {C} from './a';

export type {A} from './a';
export type {A as B} from './a';
export type {B as D} from './a';

export type E = B | C | D;
