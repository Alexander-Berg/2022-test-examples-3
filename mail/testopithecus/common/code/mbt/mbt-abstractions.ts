import { castToAny, Nullable, undefinedToNull } from '../../ys/ys';
import { TestopithecusEvent } from '../mail/logging/testopithecus-event';

export type MBTComponentType = string

/**
 * Компонент - это просто набор возможных действий плюс проверка текущего состояния
 */
export interface MBTComponent {
  /**
   * Проверяем, что то, что нарисовано на экране приложения, соответствует состоянию модели
   *
   * @param model - модель приложения
   * @param application - приложение
   */
  assertMatches(model: App, application: App): void

  getComponentType(): MBTComponentType

  tostring(): string
}

export type MBTActionType = string

export interface MBTAction {
  /**
   * Дествие может быть выполнено, только если соответствующая фича реализована в приложении.
   * Укажите тут эту фичу. Передайте null, если действие может быть выполнено на любом приложении.
   */
  supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean

  /**
   * При некоторых состояниях модели действие не может быть выполнено.
   * Например, пометить прочитанным можно только непрочитанное письмо.
   *
   * @param model - модель приложения
   */
  canBePerformed(model: App): boolean

  /**
   * Выполнение действия одновременно над приложением и над моделью
   *
   * @param model - модель приложения
   * @param application - приложение
   * @param history - где мы сейчас и где мы были до этого
   * @return компонент, на который мы должны перейти после этого действия
   */
  perform(model: App, application: App, history: MBTHistory): MBTComponent

  getActionType(): MBTActionType

  events(): TestopithecusEvent[]

  tostring(): string
}

export interface MBTHistory {
  /**
   * Текущий компонент
   */
  readonly currentComponent: MBTComponent

  /**
   * Предыдущий компонент, отличающийся от currentComponent. Нужен для возвращения назад в правильное место после perform.
   * Например, место, куда мы попадем после отправки письма зависит от того, откуда мы перешли в Compose.
   */
  readonly previousDifferentComponent: Nullable<MBTComponent>
}

export type FeatureID = string
/**
 * Собственно, платформо-зависимая реализация некоторого набора фич
 */
export interface App {
  /**
   * Фичи, которые поддерживает/реализует ваше приложение
   */
  supportedFeatures: FeatureID[]

  /**
   * Получить фичу у приложения. Перед этим надо проверить ее наличие в supportedFeatures
   *
   * @param feature - айди фичи
   */
  getFeature(feature: FeatureID): any

  /**
   * Надо вывести текущее состояние. В случае с реальным приложением это может быть ссылка на скриншот экрана.
   */
  dump(): string
}

export class Feature<T> {
  constructor(public readonly name: FeatureID, public readonly description: string = ``) {
  }

  public included(supportedFeatures: FeatureID[]): boolean {
    return supportedFeatures.includes(this.name)
  }

  public includedAll(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return this.included(modelFeatures) && this.included(applicationFeatures)
  }

  public forceCast(app: App): T {
    const t = this.castIfSupported(app)
    if (t === null) {
      throw new Error(`Feature '${this.name}' is unsupported!`)
    }
    return t!
  }

  public castIfSupported(app: App): Nullable<T> {
    if (!app.supportedFeatures.includes(this.name)) {
      return null
    }
    return this.cast(app.getFeature(this.name))
  }

  public cast(obj: any): T {
    return obj as T
  }

  public performIfSupported(app: App, action: (t: T) => void): void {
    const featured: Nullable<T> = this.castIfSupported(app);
    if (featured !== null) {
      action(featured)
    }
  }
}

export class FeatureRegistry {
  private registry: Map<FeatureID, any> = new Map()

  public constructor() {
  }

  public register<T>(feature: Feature<T>, implementation: T): FeatureRegistry {
    this.registry.set(feature.name, castToAny(implementation))
    return this
  }

  public get(feature: FeatureID): any {
    const result = undefinedToNull(this.registry.get(feature))
    if (result === null) {
      throw new Error(`Unsupported feature ${feature}`)
    }
    return result!
  }
}
