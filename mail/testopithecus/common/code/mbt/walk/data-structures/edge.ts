export class Edge<VertexType, EdgeType> {
  constructor(private from: VertexType, private to: VertexType, private action: EdgeType) {
  }

  getFrom(): VertexType {
    return this.from;
  }

  getTo(): VertexType {
    return this.to;
  }

  getAction(): EdgeType {
    return this.action;
  }
}
