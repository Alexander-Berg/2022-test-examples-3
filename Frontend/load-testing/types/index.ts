/* eslint-disable */
import { RoutesStructure } from '../configs/routes';
import { makeCommonGeneratorOptions } from '../utils/generate';

export type RouteGenerator = (...args: any[]) => string;

type RouteType = string | RouteGenerator;

type Unpromise<T> = T extends Promise<infer R> ? R : never;

export interface GeneratorOptions<T extends any = RouteType>
    extends Unpromise<ReturnType<typeof makeCommonGeneratorOptions>> {
    route: T;
    method: RequestMethod;
    ammoLabel: string;
}

export type AmmoGenerator = (options: GeneratorOptions<any>) => string;

export interface TestCaseOptions {
    name?: string;
    RPS: number;
    generator: AmmoGenerator;
    method: RequestMethod;
}

export interface RouteOptions {
    testCases: TestCaseOptions[];
}

interface LoadProfileSimple {
    warmingUp: string;
    duration: string;
    slowingDown: string;
    schema?: LoadSchema[];
}

interface LoadProfileCustom {
    warmingUp: string;
    duration?: string;
    slowingDown: string;
    schema: LoadSchema[];
}

export type LoadProfile = LoadProfileSimple | LoadProfileCustom;

export enum LoadSchemaType {
    Line = 'line',
    Const = 'const',
    Step = 'step',
}

interface LineLoadSchema {
    type: LoadSchemaType.Line;
    fromMultiplier: number;
    toMultiplier: number;
    dur: string;
}

interface ConstLoadSchema {
    type: LoadSchemaType.Const;
    loadMultiplier: number;
    dur: string;
}

interface StepLoadSchema {
    type: LoadSchemaType.Step;
    fromMultiplier: number;
    toMultiplier: number;
    step: number;
    dur: string;
}

export type LoadSchema = LineLoadSchema | ConstLoadSchema | StepLoadSchema;

export enum AutostopSchemaType {
    Time = 'time',
    Net = 'net',
    HTTP = 'http',
}

interface TimeAutostopSchema {
    type: AutostopSchemaType.Time;
    threshold: string;
    dur: string;
}

interface HttpAutostopSchema {
    type: AutostopSchemaType.HTTP;
    code: string;
    threshold: string;
    dur: string;
}

interface NetAutostopSchema {
    type: AutostopSchemaType.Net;
    code: string;
    threshold: string;
    dur: string;
}

export type AutostopSchema = TimeAutostopSchema | HttpAutostopSchema | NetAutostopSchema;

export interface TestSuite {
    target: string;
    tank: string;
    description: string;
    name: string;
    operator: string;
    ticket: string;
    loadProfile: LoadProfile;
    autostop?: AutostopSchema[];
    routes: Partial<RoutesStructure<RouteOptions>>;
}

export interface TankConfigOptions {
    target: string;
    tank: string;
    schedule: string;
    autostopOptions: string[];
    description: string;
    name: string;
    operator: string;
    ticket: string;
}
