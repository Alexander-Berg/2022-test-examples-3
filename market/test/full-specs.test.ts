import { mbiSpec } from "./full-specs/mbi-swagger";
import { githubSpec } from "./full-specs/github-swagger";
import { getSwaggerDiffBreakingChanges } from "../swagger-diff";

describe("getSwaggerDiffBreakingChanges with prod schemes", () => {
  it("without excluded checks", async () => {
    const received = await getSwaggerDiffBreakingChanges(
      mbiSpec,
      githubSpec,
      []
    );
    expect(received).toEqual({
      "response-modified": [
        {
          method: "get",
          modifiedDefinition: "ShopSurveyDTO",
          modifiedProperty: "XsurveyId",
          path: "/surveys",
          responseName: "200",
          rule: "response-modified",
          schemaPath: [
            {
              path: ["ShopSurveysDTO", "shopSurveys"]
            }
          ]
        }
      ]
    });
  });
});
