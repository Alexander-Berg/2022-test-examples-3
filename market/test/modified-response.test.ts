import { Schema } from "swagger-schema-official";

import {
  specBuilder,
  operationBuilder,
  pathItemBuilder,
  responseBuilder,
  schemaBuilder
} from "../utils/spec-builder";
import { getSwaggerDiffBreakingChanges } from "../swagger-diff";

const FulfillmentServiceDTO: Schema = {
  type: "object",
  properties: {
    id: {
      type: "integer",
      format: "int64"
    },
    name: {
      type: "string"
    },
    warehouse: {
      $ref: "#/definitions/WarehouseDTO"
    }
  },
  title: "FulfillmentServiceDTO"
};

const withPathAndDef = specBuilder
  .withPath(
    "/ff-service",
    pathItemBuilder.withOperation(
      "get",
      operationBuilder.withResponse(
        "200",
        responseBuilder
          .withDescription("get a ff service")
          .withResponseBody(
            schemaBuilder.withRef("#/definitions/FulfillmentServiceDTO")
          )
      )
    )
  )
  .withDefinition(
    "FulfillmentServiceDTO",
    schemaBuilder.fromSchema(FulfillmentServiceDTO)
  );

test("sanity check", async () => {
  const swagger = withPathAndDef.build();

  const diff = await getSwaggerDiffBreakingChanges(swagger, swagger, []);
  expect(diff).toMatchInlineSnapshot(`Object {}`);
});

test("when dto is modified", async () => {
  // Поменяем поле в респонсе ручки (удалим id из GetFFService) и в зависимом дто:
  // удалим id из WarehouseDTO, на который ссылается GetFFService
  const baseSchemaBuilder = specBuilder
    .withPath(
      "/get-ff-service",
      pathItemBuilder.withOperation(
        "get",
        operationBuilder.withResponse(
          "200",
          responseBuilder.withRef("#/definitions/GetFFService")
        )
      )
    )
    .withDefinition(
      "WarehouseDTO",
      schemaBuilder
        .fromSchema({
          type: "object"
        })
        .withProperty("id", schemaBuilder.fromSchema({ type: "number" }))
    );

  const baseResponse: Schema = {
    type: "object",
    properties: {
      id: {
        type: "integer",
        format: "int64"
      },
      name: {
        type: "string"
      },
      warehouse: {
        $ref: "#/definitions/WarehouseDTO"
      }
    }
  };

  const before = baseSchemaBuilder
    .withDefinition("GetFFService", schemaBuilder.fromSchema(baseResponse))
    .build();

  const after = baseSchemaBuilder
    .withDefinition(
      "GetFFService",
      schemaBuilder
        .fromSchema(baseResponse)
        .withoutProperty("id")
        .withProperty(
          "id1",
          schemaBuilder.fromSchema(baseResponse.properties!.id)
        )
    )
    .withDefinition(
      "WarehouseDTO",
      schemaBuilder.fromSchema({
        type: "object",
        properties: {}
      })
    )
    .build();

  const checkerDiff = await getSwaggerDiffBreakingChanges(before, after, []);
  expect(checkerDiff).toMatchInlineSnapshot(`
                    Object {
                      "response-modified": Array [
                        Object {
                          "method": "get",
                          "modifiedDefinition": "WarehouseDTO",
                          "modifiedProperty": "id",
                          "path": "/get-ff-service",
                          "responseName": "200",
                          "rule": "response-modified",
                          "schemaPath": Array [
                            Link {
                              "path": Array [
                                "GetFFService",
                                "warehouse",
                              ],
                            },
                          ],
                        },
                        Object {
                          "method": "get",
                          "modifiedDefinition": "GetFFService",
                          "modifiedProperty": "id",
                          "path": "/get-ff-service",
                          "responseName": "200",
                          "rule": "response-modified",
                          "schemaPath": Array [],
                        },
                      ],
                    }
          `);
});

test("when a modified leaf dto breaks multiple endpoints", async () => {
  // заведем три ручки со своими дто (dto_a, dto_b, dto_c), которые будут зависеть
  // от shared_dto. В shared_dto удалим name
  const baseBuilder = specBuilder
    .withPath(
      "/a",
      pathItemBuilder.withOperation(
        "get",
        operationBuilder.withResponse(
          "200",
          responseBuilder.withRef("#/definitions/dto_a")
        )
      )
    )
    .withPath(
      "/b",
      pathItemBuilder.withOperation(
        "get",
        operationBuilder.withResponse(
          "200",
          responseBuilder.withRef("#/definitions/dto_b")
        )
      )
    )
    .withPath(
      "/c",
      pathItemBuilder.withOperation(
        "get",
        operationBuilder.withResponse(
          "200",
          responseBuilder.withRef("#/definitions/dto_c")
        )
      )
    )
    .withDefinition(
      "dto_a",
      schemaBuilder.withProperty(
        "a_field",
        schemaBuilder.withRef("#/definitions/shared_dto")
      )
    )
    .withDefinition(
      "dto_b",
      schemaBuilder.withProperty(
        "b_field",
        schemaBuilder.withRef("#/definitions/shared_dto")
      )
    )
    .withDefinition(
      "dto_c",
      schemaBuilder.withProperty(
        "c_field",
        schemaBuilder.withRef("#/definitions/shared_dto")
      )
    );

  const before = baseBuilder
    .withDefinition(
      "shared_dto",
      schemaBuilder.fromSchema(FulfillmentServiceDTO)
    )
    .build();
  const after = baseBuilder
    .withDefinition(
      "shared_dto",
      schemaBuilder.fromSchema(FulfillmentServiceDTO).withoutProperty("name")
    )
    .build();

  const diff = await getSwaggerDiffBreakingChanges(before, after, []);
  expect(diff).toMatchInlineSnapshot(`
            Object {
              "response-modified": Array [
                Object {
                  "method": "get",
                  "modifiedDefinition": "shared_dto",
                  "modifiedProperty": "name",
                  "path": "/a",
                  "responseName": "200",
                  "rule": "response-modified",
                  "schemaPath": Array [
                    Link {
                      "path": Array [
                        "dto_a",
                        "a_field",
                      ],
                    },
                  ],
                },
                Object {
                  "method": "get",
                  "modifiedDefinition": "shared_dto",
                  "modifiedProperty": "name",
                  "path": "/b",
                  "responseName": "200",
                  "rule": "response-modified",
                  "schemaPath": Array [
                    Link {
                      "path": Array [
                        "dto_b",
                        "b_field",
                      ],
                    },
                  ],
                },
                Object {
                  "method": "get",
                  "modifiedDefinition": "shared_dto",
                  "modifiedProperty": "name",
                  "path": "/c",
                  "responseName": "200",
                  "rule": "response-modified",
                  "schemaPath": Array [
                    Link {
                      "path": Array [
                        "dto_c",
                        "c_field",
                      ],
                    },
                  ],
                },
              ],
            }
      `);
});
