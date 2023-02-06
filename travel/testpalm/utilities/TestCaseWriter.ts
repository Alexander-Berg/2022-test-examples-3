import fs from 'fs-extra';
import path from 'path';
import YAML from 'js-yaml';

import {
    COMMON_DIR_NAME,
    DESKTOP_DEVICE_NAME,
    DESKTOP_DIR_NAME,
    DEVICE_DEFINITION_TITLE,
    FEATURE_DEFINITION_TITLE,
    FEATURE_KEYS,
    MOBILE_DEVICE_NAME,
    MOBILE_DIR_NAME,
    PAGE_DEFINITION_TITLE,
    PAGE_KEYS,
} from './constants';

import {
    ITestCase,
    ITestCaseStep,
    ITestDefinition,
    TPreparedTestCaseStep,
} from './types';

interface ITestCaseWriterResult {
    created: number;
}

type TStepPart =
    | {
          do: string;
      }
    | {
          assert: string;
      };

const DO_KEYWORD = 'do:';
const ASSERT_KEYWORD = 'assert:';

export class TestCaseWriter {
    private folder: string;
    private definitions: ITestDefinition[];

    constructor(folder: string, definitions: ITestDefinition[]) {
        this.folder = folder;
        this.definitions = definitions;
    }

    async write(testCases: ITestCase[]): Promise<ITestCaseWriterResult> {
        const result = {
            created: 0,
        };

        for (const testCase of testCases) {
            const {id, name} = testCase;
            const dirName = this.prepareDirName(testCase);
            const fileName = `[${id}] ${name}`
                .replace(/\s+/g, ' ')
                .replace(/[:<>]/g, '')
                .replace(/\//g, ' или ')
                .replace(/"/g, "'");

            const filePath = path.join(dirName, fileName + '.testpalm.yaml');

            const preparedTestCase = this.prepareTestCase(testCase);

            fs.writeFileSync(filePath, YAML.dump(preparedTestCase));

            result.created += 1;
        }

        return result;
    }

    private prepareDirName(testCase: ITestCase): string {
        const device = this.findDeviceDirName(testCase);
        const feature = this.findFeatureKey(testCase);
        const page = this.findPageKey(testCase);
        const suiteDir = path.join(this.folder, device, feature, page);

        fs.ensureDirSync(suiteDir);

        return suiteDir;
    }

    private prepareTestCase(testCase: ITestCase): {
        feature: string;
        type: string;
        description: string;
        specs: Record<string, TPreparedTestCaseStep[]>;
    } {
        const feature = this.findFeature(testCase);
        const type = this.findType(testCase);
        const description = this.prepareDescription(testCase);
        const specs = this.prepareSpecs(testCase);
        const attributes = this.prepareAttributes(testCase);

        return {
            feature,
            type,
            description,
            specs,
            ...attributes,
        };
    }

    private findDeviceDirName(testCase: ITestCase): string {
        const values = this.findAttributeValues(
            DEVICE_DEFINITION_TITLE,
            testCase,
        );

        if (!values) {
            return COMMON_DIR_NAME;
        }

        const isMobile = values.some(v => v === MOBILE_DEVICE_NAME);
        const isDesktop = values.some(v => v === DESKTOP_DEVICE_NAME);

        if (isMobile && isDesktop) {
            return COMMON_DIR_NAME;
        }

        if (isMobile) {
            return MOBILE_DIR_NAME;
        }

        if (isDesktop) {
            return DESKTOP_DIR_NAME;
        }

        return COMMON_DIR_NAME;
    }

    private findPageKey = (testCase: ITestCase): string => {
        const feature = this.findPage(testCase);

        return PAGE_KEYS[feature] || feature;
    };

    private findPage = (testCase: ITestCase): string =>
        this.findAttributeValue(PAGE_DEFINITION_TITLE, testCase);

    private findFeatureKey = (testCase: ITestCase): string => {
        const feature = this.findFeature(testCase);

        return FEATURE_KEYS[feature] || feature;
    };

    private findFeature = (testCase: ITestCase): string =>
        this.findAttributeValue(FEATURE_DEFINITION_TITLE, testCase);

    private findType = (testCase: ITestCase): string =>
        this.findAttributeValue(PAGE_DEFINITION_TITLE, testCase);

    private findAttributeValues(
        definitionTitle: string,
        testCase: ITestCase,
    ): string[] {
        const definition = this.definitions.find(
            d => d.title === definitionTitle,
        );

        if (!definition) {
            return [];
        }

        if (!testCase.attributes) {
            return [];
        }

        const attribute = testCase.attributes[definition.id];

        if (!attribute) {
            return [];
        }

        return attribute;
    }

    private findAttributeValue(
        definitionTitle: string,
        testCase: ITestCase,
    ): string {
        const values = this.findAttributeValues(definitionTitle, testCase);

        if (!values) {
            return '';
        }

        return values[0] || '';
    }

    private prepareDescription(testCase: ITestCase): string {
        const {preconditions, description} = testCase;
        const descList = [];

        if (description) {
            descList.push(this.capitalize(this.prepareText(description)));
        }

        if (preconditions) {
            descList.push(this.capitalize(this.prepareText(preconditions)));
        }

        return descList.join('\n');
    }

    private prepareSpecs(
        testCase: ITestCase,
    ): Record<string, TPreparedTestCaseStep[]> {
        const {name, stepsExpects} = testCase;

        // если есть шаги, то разбивем их на части  do/assert
        if (stepsExpects && stepsExpects.length) {
            const specName = this.capitalize(this.prepareText(name));

            return {
                [specName]: stepsExpects.reduce(
                    (
                        acc: TPreparedTestCaseStep[],
                        {step, expect}: ITestCaseStep,
                    ) => {
                        if (step) {
                            const stepParts = this.prepareStep(step);

                            for (let i = 0; i < stepParts.length; i++) {
                                acc.push(stepParts[i]);
                            }
                        }

                        if (expect) {
                            acc.push({
                                assert: this.prepareText(expect),
                            });
                        }

                        return acc;
                    },
                    [],
                ),
            };
        }

        return {};
    }

    // в step иногда пишут do/assert
    private prepareStep(step: string): TStepPart[] {
        const parts = step.split(DO_KEYWORD);

        return parts.reduce<TStepPart[]>((result, part) => {
            const parts2 = part.split(ASSERT_KEYWORD);

            if (parts2[0] && parts2[0].trim()) {
                result.push({do: this.prepareText(parts2[0])});
            }

            for (let i = 1; i < parts2.length; i++) {
                result.push({assert: this.prepareText(parts2[i])});
            }

            return result;
        }, []);
    }

    private prepareAttributes(
        testCase: ITestCase,
    ): Record<string, string | string[]> {
        const {attributes} = testCase;

        if (!attributes) {
            return {};
        }

        const attributeIds = Object.keys(attributes);

        return attributeIds.reduce(
            (result: Record<string, string | string[]>, attributeId) => {
                const attributeDefinition = this.definitions.find(
                    a => a.id === attributeId,
                );

                const attributeValue = this.tryTakeArrayElement(
                    attributes[attributeId],
                );

                if (attributeDefinition && attributeValue) {
                    result[attributeDefinition.title] = attributeValue;
                }

                return result;
            },
            {},
        );
    }

    private tryTakeArrayElement(values: string[]): string | string[] {
        if (values.length < 2) {
            return values[0];
        }

        return values;
    }

    private capitalize(text: string): string {
        if (!text) {
            return text;
        }

        return text.slice(0, 1).toUpperCase() + text.slice(1);
    }

    private prepareText(text: string): string {
        if (!text) {
            return text;
        }

        return text.trim();
    }
}
