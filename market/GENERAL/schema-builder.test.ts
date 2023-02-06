import { Schema } from "swagger-schema-official";

import { schemaBuilder } from "./schema-builder";

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

describe("schemaBuilder", () => {
  it("should build schema from existing schema", () => {
    expect(schemaBuilder.fromSchema(FulfillmentServiceDTO).build())
      .toMatchInlineSnapshot(`
            Object {
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
            }
        `);
  });

  it("should be able to remove property", () => {
    const schema = schemaBuilder
      .fromSchema(FulfillmentServiceDTO)
      .withoutProperty("name")
      .build();

    expect(schema).toMatchInlineSnapshot(`
      Object {
        "properties": Object {
          "id": Object {
            "format": "int64",
            "properties": Object {},
            "type": "integer",
          },
          "warehouse": Object {
            "$ref": "#/definitions/WarehouseDTO",
            "properties": Object {},
          },
        },
        "title": "FulfillmentServiceDTO",
        "type": "object",
      }
    `);
  });
});
