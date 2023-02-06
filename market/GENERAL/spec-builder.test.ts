import {
  specBuilder,
  pathItemBuilder,
  operationBuilder,
  responseBuilder,
  schemaBuilder
} from "./";
import { Schema } from "swagger-schema-official";

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

test("spec-builder", () => {
  const swagger = specBuilder
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
    )
    .build();

  expect(swagger).toMatchInlineSnapshot(`
    Object {
      "definitions": Object {
        "FulfillmentServiceDTO": Object {
          "properties": Object {
            "id": Object {
              "format": "int64",
              "properties": Object {},
              "type": "integer",
            },
            "name": Object {
              "properties": Object {},
              "type": "string",
            },
            "warehouse": Object {
              "$ref": "#/definitions/WarehouseDTO",
              "properties": Object {},
            },
          },
          "title": "FulfillmentServiceDTO",
          "type": "object",
        },
      },
      "info": Object {
        "title": "",
        "version": "",
      },
      "paths": Object {
        "/ff-service": Object {
          "get": Object {
            "description": "Operation description",
            "operationId": "Operation Id",
            "responses": Object {
              "200": Object {
                "description": "get a ff service",
                "schema": Object {
                  "$ref": "#/definitions/FulfillmentServiceDTO",
                },
              },
            },
          },
        },
      },
      "swagger": "2.0",
    }
  `);
});
