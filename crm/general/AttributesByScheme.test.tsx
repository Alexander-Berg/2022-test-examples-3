// import React from "react";
// import { render, screen } from "@testing-library/react";
// import { TestBed } from "TestBed";
// import { AttributesByScheme } from "./AttributesByScheme";
// import { AttributesScheme } from "./AttributesByScheme.types";

// jest.mock("react-redux", () => ({
//     ...(jest.requireActual("react-redux") as object),
//     useSelector: () => undefined,
// }));

// describe("AttributesByScheme", () => {
//     it("renders AccountInput", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "AccountInput",
//                 name: "1",
//                 label: "AccountInput",
//             },
//         ];

//         render(
//             <TestBed>
//                 <AttributesByScheme scheme={scheme} />
//             </TestBed>
//         );

//         expect(screen.getByText("AccountInput")).toBeInTheDocument();
//     });

//     it("renders Categorization", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Categorization",
//                 name: "1",
//                 label: "Categorization",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(
//             screen.getAllByText("Categorization").length
//         ).toBeGreaterThanOrEqual(1);
//     });

//     it("renders DateTimePicker", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "DateTimePicker",
//                 name: "1",
//                 label: "DateTimePicker",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("DateTimePicker")).toBeInTheDocument();
//     });

//     it("renders Grants", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Grants",
//                 name: "1",
//                 label: "Grants",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("Grants")).toBeInTheDocument();
//     });

//     it("renders Select", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Select",
//                 name: "1",
//                 label: "Select",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getAllByText("Select").length).toBeGreaterThanOrEqual(1);
//     });

//     it("renders Skills", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Skills",
//                 name: "1",
//                 label: "Skills",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("Skills")).toBeInTheDocument();
//     });

//     it("renders StaffUserInput", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "StaffUserInput",
//                 name: "1",
//                 label: "StaffUserInput",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("StaffUserInput")).toBeInTheDocument();
//     });

//     it("renders Suggest", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Suggest",
//                 name: "1",
//                 label: "Suggest",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("Suggest")).toBeInTheDocument();
//     });

//     it("renders Tags", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Tags",
//                 name: "1",
//                 label: "Tags",
//             },
//         ];

//         render(
//             <TestBed>
//                 <AttributesByScheme scheme={scheme} />
//             </TestBed>
//         );

//         expect(screen.getByText("Tags")).toBeInTheDocument();
//     });

//     it("renders Textinput", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "TextInput",
//                 name: "1",
//                 label: "Textinput",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("Textinput")).toBeInTheDocument();
//     });

//     it("renders Timers", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Timers",
//                 name: "1",
//                 label: "Timers",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("Timers")).toBeInTheDocument();
//     });

//     it("renders OpportunitiesInput", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "OpportunitiesInput",
//                 name: "1",
//                 label: "OpportunitiesInput",
//             },
//         ];

//         const getOpportunitiesInput: GetOpportunitiesInput = () => {
//             return {
//                 value: [],
//             };
//         };

//         render(
//             <AttributesByScheme
//                 scheme={scheme}
//                 getOpportunitiesInput={getOpportunitiesInput}
//             />
//         );

//         expect(screen.getByText("OpportunitiesInput")).toBeInTheDocument();
//     });

//     it("renders Checkbox", () => {
//         const scheme: AttributesScheme = [
//             {
//                 component: "Checkbox",
//                 name: "1",
//                 label: "Checkbox",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} />);

//         expect(screen.getByText("Checkbox")).toBeInTheDocument();
//     });

//     it("forwards props from getCommon", () => {
//         const values = {
//             1: "test 1",
//             2: "test 2",
//             3: "test 3",
//         };

//         const getCommon: GetCommon = jest.fn((attribute) => {
//             return {
//                 value: values[attribute.name],
//             };
//         });
//         const scheme: AttributesScheme = [
//             {
//                 component: "TextInput",
//                 name: "1",
//                 label: "Textinput 1",
//             },
//             {
//                 component: "TextInput",
//                 name: "2",
//                 label: "Textinput 2",
//             },
//             {
//                 component: "TextInput",
//                 name: "3",
//                 label: "Textinput 3",
//             },
//         ];

//         render(<AttributesByScheme scheme={scheme} getCommon={getCommon} />);

//         expect(getCommon).toBeCalledTimes(3);
//         expect(screen.getAllByText("test 1").length).toBeGreaterThanOrEqual(1);
//         expect(screen.getAllByText("test 2").length).toBeGreaterThanOrEqual(1);
//         expect(screen.getAllByText("test 3").length).toBeGreaterThanOrEqual(1);
//     });

//     it("overrides components", () => {
//         const componentTypes = [
//             "DateTimePicker",
//             "StaffUserInput",
//             "Suggest",
//             "Categorization",
//             "TextInput",
//             "Select",
//             "AccountInput",
//             "Tags",
//             "Grants",
//             "Timers",
//             "Skills",
//         ] as const;
//         const textMap: Record<AttributeComponentType, string> = {
//             DateTimePicker: "date time picker",
//             StaffUserInput: "staff user input",
//             TextInput: "textinput",
//             Suggest: "suggest",
//             Categorization: "categorization",
//             Select: "select",
//             AccountInput: "account input",
//             Tags: "tags",
//             Grants: "grants",
//             Timers: "timers",
//             Skills: "skills",
//             OpportunitiesInput: "opportunities input",
//             Checkbox: "checkbox",
//         };

//         const renderComponents = componentTypes.reduce(
//             (acc, component) => ({
//                 ...acc,
//                 [component]: () => <div>{textMap[component]}</div>,
//             }),
//             {} as RenderComponents
//         );

//         const scheme: AttributesScheme = componentTypes.map(
//             (component, index) => ({
//                 component,
//                 name: index.toString(),
//                 label: "",
//             })
//         );

//         render(
//             <AttributesByScheme
//                 renderComponents={renderComponents}
//                 scheme={scheme}
//             />
//         );

//         componentTypes.forEach((component) => {
//             expect(screen.getByText(textMap[component])).toBeInTheDocument();
//         });
//     });

//     it("calls onCommonChange for component when there is no change-handler for it", () => {
//         const onCommonChange: CommonChangeHandler = jest.fn();

//         let emitChange: Function = () => {};
//         const renderComponents: Partial<RenderComponents> = {
//             TextInput: (props) => {
//                 emitChange = props.onChange;
//                 return null;
//             },
//         };

//         const scheme: AttributesScheme = [
//             {
//                 component: "TextInput",
//                 name: "1",
//                 label: "Textinput 1",
//             },
//         ];

//         render(
//             <AttributesByScheme
//                 renderComponents={renderComponents}
//                 scheme={scheme}
//                 onCommonChange={onCommonChange}
//             />
//         );

//         emitChange("");

//         expect(onCommonChange).toBeCalled();
//     });
// });
