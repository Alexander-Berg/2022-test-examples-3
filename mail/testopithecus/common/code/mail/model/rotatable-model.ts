import { Rotatable } from '../mail-features';

export class RotatableModel implements Rotatable {
  public landscape: boolean = false

  public rotateToLandscape(): void {
    this.landscape = true
  }

  public rotateToPortrait(): void {
    this.landscape = false
  }

  public copy(): RotatableModel {
    const copy = new RotatableModel()
    copy.landscape = this.landscape
    return copy
  }

  public isInLandscape(): boolean {
    return this.landscape;
  }
}
