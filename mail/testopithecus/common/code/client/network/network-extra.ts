export class NetworkExtra {
  public constructor(
    public readonly foreground: boolean,
    public readonly uuid: string,
  ) { }

  public static mockExtra(): NetworkExtra {
    return new NetworkExtra(false, '')
  }

}
