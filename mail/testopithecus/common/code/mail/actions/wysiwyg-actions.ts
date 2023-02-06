import { Int32 } from '../../../ys/ys'
import { BaseSimpleAction } from '../../mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../mbt/mbt-abstractions'
import { ComposeComponent } from '../components/compose-component'
import { Testopithecus } from '../logging/events/testopithecus'
import { TestopithecusEvent } from '../logging/testopithecus-event'
import {
  WYSIWIG,
  WysiwygFeature,
} from '../mail-features'

export abstract class WysiwygBaseAction extends BaseSimpleAction<WYSIWIG, MBTComponent> {
  constructor(type: MBTActionType) {
    super(type)
  }

  public requiredFeature(): Feature<WYSIWIG> {
    return WysiwygFeature.get
  }

  public events(): TestopithecusEvent[] {
    return [Testopithecus.stubEvent()]
  }
}

export class ClearFormatting extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'ClearFormatting'

  constructor(private from: Int32, private to: Int32) {
    super(ClearFormatting.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.clearFormatting(this.from, this.to)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `ClearFormatting(from=${this.from}, to=${this.to})`
  }

}

export class SetStrong extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'SetStrong'

  constructor(private from: Int32, private to: Int32) {
    super(SetStrong.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.setStrong(this.from, this.to)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `SetStrong(from=${this.from}, to=${this.to})`
  }

}

export class SetItalic extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'SetItalic'

  constructor(private from: Int32, private to: Int32) {
    super(SetItalic.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.setItalic(this.from, this.to)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `SetItalic(from=${this.from}, to=${this.to})`
  }

}

export class AppendToBody extends WysiwygBaseAction {
  public static readonly type: MBTActionType = 'AppendToBody'

  constructor(private index: Int32, private text: string) {
    super(AppendToBody.type)
  }

  public performImpl(modelOrApplication: WYSIWIG, currentComponent: MBTComponent): MBTComponent {
    modelOrApplication.appendText(this.index, this.text)
    return new ComposeComponent()
  }

  public tostring(): string {
    return `AppendToBody(index=${this.index}, text=${this.text})`
  }

}
