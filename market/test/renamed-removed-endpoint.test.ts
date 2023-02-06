import { clone } from "ramda";

import { getSwaggerDiffBreakingChanges } from "../swagger-diff";
import { leftSpec } from "./light-specs/left";

const rule = "endpoint-was-removed";

describe("getSwaggerDiffBreakingChanges", () => {
  it("should handle removed path with all its methods", async () => {
    const sourceSpec = clone(leftSpec);
    sourceSpec.paths["/a"] = {
      get: {
        responses: {}
      },
      post: {
        responses: {}
      }
    };
    sourceSpec.paths["/b"] = {
      get: {
        responses: {}
      },
      post: {
        responses: {}
      }
    };

    const changes = await getSwaggerDiffBreakingChanges(
      sourceSpec,
      leftSpec,
      []
    );

    expect(changes).toEqual({
      [rule]: [
        {
          rule,
          method: "get",
          path: "/a"
        },
        {
          rule,
          method: "post",
          path: "/a"
        },
        {
          rule,
          method: "get",
          path: "/b"
        },
        {
          rule,
          method: "post",
          path: "/b"
        }
      ]
    });
  });

  it("should handle removed method", async () => {
    const rightSpec = clone(leftSpec);
    delete rightSpec.paths["/parameter-became-required"].get;
    rightSpec.paths["/new-endpoint"] = {
      get: leftSpec.paths["/parameter-became-required"].get
    };

    delete rightSpec.paths["/parameter-became-renamed"].get;
    rightSpec.paths["/new-endpoint-2"] = {
      get: leftSpec.paths["/parameter-became-renamed"].get
    };

    const changes = await getSwaggerDiffBreakingChanges(
      leftSpec,
      rightSpec,
      []
    );

    expect(changes).toEqual({
      "endpoint-was-removed": [
        {
          rule,
          method: "get",
          path: "/parameter-became-required"
        },
        {
          rule,
          method: "get",
          path: "/parameter-became-renamed"
        }
      ]
    });
  });
});
