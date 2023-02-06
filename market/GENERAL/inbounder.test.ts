import { Spec, Schema } from "swagger-schema-official";

import {
  specBuilder,
  schemaBuilder,
  responseBuilder,
  operationBuilder,
  pathItemBuilder
} from "../spec-builder";
import { Inbounder, Link } from "./inbounder";

describe("Inbounder", () => {
  it.skip('should catch modifications in survey dto', () => {
    // specBuilder
  })

  it("should handle arrays", () => {
    const spec: Spec = specBuilder
      .withDefinition(
        "ListDTO",
        schemaBuilder.fromSchema({
          type: "array",
          items: {
            $ref: "#/definitions/ArrayItem"
          }
        })
      )
      .withDefinition(
        "A",
        schemaBuilder.fromSchema({
          type: "object",
          properties: { baz: { type: "number" } }
        })
      )
      .withDefinition(
        "ArrayItem",
        schemaBuilder.fromSchema({
          type: "object",
          properties: {
            foo: { type: "string" },
            qwe: { $ref: "#/definitions/A" }
          }
        })
      )
      .withPath(
        "/test-path",
        pathItemBuilder.withOperation(
          "get",
          operationBuilder.withResponse(
            "200",
            responseBuilder.withRef("#/definitions/ListDTO")
          )
        )
      )
      .build();

    const inbounder = new Inbounder(spec);

    expect(inbounder.findAffectedOperations("ArrayItem"))
      .toMatchInlineSnapshot(`
                  Array [
                    Array [
                      Link {
                        "path": Array [
                          "paths",
                          "/test-path",
                          "get",
                          "200",
                        ],
                      },
                      Link {
                        "path": Array [
                          "ListDTO",
                        ],
                      },
                    ],
                  ]
            `);
    expect(inbounder.findAffectedOperations("A")).toMatchInlineSnapshot(`
                  Array [
                    Array [
                      Link {
                        "path": Array [
                          "paths",
                          "/test-path",
                          "get",
                          "200",
                        ],
                      },
                      Link {
                        "path": Array [
                          "ListDTO",
                        ],
                      },
                      Link {
                        "path": Array [
                          "ArrayItem",
                          "qwe",
                        ],
                      },
                    ],
                  ]
            `);
  });

  it("should handle outgoing link via direct $ref in response", () => {
    const spec: Spec = specBuilder
      .withDefinition("a", schemaBuilder.fromSchema({ type: "number" }))
      .withPath(
        "/a-path",
        pathItemBuilder.withOperation(
          "get",
          operationBuilder.withResponse(
            "200",
            responseBuilder.withRef("#/definitions/a")
          )
        )
      )
      .build();

    const inbounder = new Inbounder(spec);
    expect(inbounder.findAffectedOperations("a")).toMatchInlineSnapshot(`
                                                      Array [
                                                        Array [
                                                          Link {
                                                            "path": Array [
                                                              "paths",
                                                              "/a-path",
                                                              "get",
                                                              "200",
                                                            ],
                                                          },
                                                        ],
                                                      ]
                                    `);
  });

  it("should handle outgoing link via schema.$ref", () => {
    const spec: Spec = specBuilder
      .withDefinition("a", schemaBuilder.fromSchema({ type: "number" }))
      .withPath(
        "/a-path",
        pathItemBuilder.withOperation(
          "get",
          operationBuilder.withResponse(
            "200",
            responseBuilder.withResponseBody(
              schemaBuilder.withRef("#/definitions/a")
            )
          )
        )
      )
      .build();

    const inbounder = new Inbounder(spec);
    expect(inbounder.findAffectedOperations("a")).toMatchInlineSnapshot(`
                                                                  Array [
                                                                    Array [
                                                                      Link {
                                                                        "path": Array [
                                                                          "paths",
                                                                          "/a-path",
                                                                          "get",
                                                                          "200",
                                                                        ],
                                                                      },
                                                                    ],
                                                                  ]
                                            `);
  });

  it("should handle links starting at path", () => {
    const spec: Spec = specBuilder
      .withPath(
        "/get-my-id",
        pathItemBuilder.withOperation(
          "get",
          operationBuilder.withResponse(
            "200",
            responseBuilder.withRef("#/definitions/a")
          )
        )
      )
      .withDefinition(
        "a",
        schemaBuilder.withProperty(
          "a_prop",
          schemaBuilder.withRef("#/definitions/b")
        )
      )
      .withDefinition("b", schemaBuilder.fromSchema({ type: "object" }))
      .build();

    expect(spec).toMatchInlineSnapshot(`
      Object {
        "definitions": Object {
          "a": Object {
            "properties": Object {
              "a_prop": Object {
                "$ref": "#/definitions/b",
              },
            },
            "type": "object",
          },
          "b": Object {
            "type": "object",
          },
        },
        "info": Object {
          "title": "",
          "version": "",
        },
        "paths": Object {
          "/get-my-id": Object {
            "get": Object {
              "description": "Operation description",
              "operationId": "Operation Id",
              "responses": Object {
                "200": Object {
                  "$ref": "#/definitions/a",
                  "description": "response description",
                },
              },
            },
          },
        },
        "swagger": "2.0",
      }
    `);

    const inbounder = new Inbounder(spec);
    expect(inbounder.getInboundLinks("b")).toEqual([new Link("a", "a_prop")]);
    expect(inbounder.getInboundLinks("a")).toEqual([
      new Link("paths", "/get-my-id", "get", "200")
    ]);

    expect(inbounder.findAffectedOperations("b")).toMatchInlineSnapshot(`
                                                                              Array [
                                                                                Array [
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "paths",
                                                                                      "/get-my-id",
                                                                                      "get",
                                                                                      "200",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "a",
                                                                                      "a_prop",
                                                                                    ],
                                                                                  },
                                                                                ],
                                                                              ]
                                                    `);
  });

  it("should handle multilple inbound links", () => {
    const e = schemaBuilder.fromSchema({
      title: "e",
      type: "object",
      properties: {
        id: { type: "integer" }
      }
    });
    const d = schemaBuilder
      .fromSchema({
        title: "d",
        type: "object"
      })
      .withProperty("d_prop1", schemaBuilder.withRef("#/definitions/e"));
    const [a, b, c] = ["a", "b", "c"].map(title =>
      schemaBuilder
        .fromSchema({ title, type: "object" })
        .withProperty(
          `${title}_props`,
          schemaBuilder.withRef("#/definitions/d")
        )
    );
    const spec: Spec = specBuilder
      .withPath(
        "/get-a",
        pathItemBuilder.withOperation(
          "get",
          operationBuilder.withResponse(
            "200",
            responseBuilder.withRef("#/definitions/a")
          )
        )
      )
      .withPath(
        "/post-b",
        pathItemBuilder.withOperation(
          "post",
          operationBuilder.withResponse(
            "400",
            responseBuilder.withRef("#/definitions/b")
          )
        )
      )
      .withPath(
        "/put-c",
        pathItemBuilder.withOperation(
          "put",
          operationBuilder.withResponse(
            "404",
            responseBuilder.withRef("#/definitions/c")
          )
        )
      )
      .withDefinition("e", e)
      .withDefinition("d", d)
      .withDefinition("a", a)
      .withDefinition("b", b)
      .withDefinition("c", c)
      .build();

    const inbounder = new Inbounder(spec);
    expect(inbounder.getInboundLinks("e")).toEqual([new Link("d", "d_prop1")]);
    expect(inbounder.getInboundLinks("d")).toEqual([
      new Link("a", "a_props"),
      new Link("b", "b_props"),
      new Link("c", "c_props")
    ]);

    expect(inbounder.findAffectedOperations("e")).toMatchInlineSnapshot(`
                                                                              Array [
                                                                                Array [
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "paths",
                                                                                      "/get-a",
                                                                                      "get",
                                                                                      "200",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "a",
                                                                                      "a_props",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "d",
                                                                                      "d_prop1",
                                                                                    ],
                                                                                  },
                                                                                ],
                                                                                Array [
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "paths",
                                                                                      "/post-b",
                                                                                      "post",
                                                                                      "400",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "b",
                                                                                      "b_props",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "d",
                                                                                      "d_prop1",
                                                                                    ],
                                                                                  },
                                                                                ],
                                                                                Array [
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "paths",
                                                                                      "/put-c",
                                                                                      "put",
                                                                                      "404",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "c",
                                                                                      "c_props",
                                                                                    ],
                                                                                  },
                                                                                  Link {
                                                                                    "path": Array [
                                                                                      "d",
                                                                                      "d_prop1",
                                                                                    ],
                                                                                  },
                                                                                ],
                                                                              ]
                                                    `);
  });

  it("should handle no affected paths", () => {
    const spec: Spec = specBuilder
      .withDefinition(
        "a",
        schemaBuilder
          .fromSchema({
            type: "object",
            title: "a"
          })
          .withProperty("a_prop1", schemaBuilder.withRef("#/definitions/b"))
      )
      .withDefinition(
        "b",
        schemaBuilder.fromSchema({
          type: "object",
          properties: {
            id: {
              type: "integer"
            }
          },
          title: "b"
        })
      )
      .build();

    const inbounder = new Inbounder(spec);
    expect(inbounder.getInboundLinks("a")).toEqual([]);
    expect(inbounder.getInboundLinks("b")).toEqual([new Link("a", "a_prop1")]);

    expect(inbounder.findAffectedOperations("b")).toEqual([]);
  });
});
