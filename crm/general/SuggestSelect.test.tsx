// import React from "react";
// import {
//     render,
//     screen,
//     getByLabelText,
//     cleanup,
//     waitFor,
// } from "@testing-library/react/pure";
// import userEvent from "@testing-library/user-event";
// import { Grid } from "../components/Grid";
// import { StatefulSuggestSelect } from "./StatefulSuggestSelect";

// describe("Attribute2/SuggestSelect", () => {
//     it("renders label", () => {
//         render(
//             <Grid>
//                 <StatefulSuggestSelect label="label" defaultValue={null} />
//             </Grid>
//         );

//         const readingArea = screen.getByLabelText("reading area");
//         const label = getByLabelText(readingArea, "label");
//         expect(label).toBeVisible();

//         cleanup();
//     });

//     it("renders value", () => {
//         render(
//             <Grid>
//                 <StatefulSuggestSelect
//                     label="label"
//                     defaultValue={{ id: 1, text: "value" }}
//                 />
//             </Grid>
//         );

//         const readingArea = screen.getByLabelText("reading area");
//         const value = getByLabelText(readingArea, "value");
//         expect(value).toBeVisible();

//         cleanup();
//     });

//     describe("after click", () => {
//         beforeEach(() => {
//             const options = [
//                 { id: 1, text: "test option 1" },
//                 { id: 2, text: "test option 2" },
//                 { id: 3, text: "test option 3" },
//             ];
//             const loadOptions = (text: string) => {
//                 return options.filter((option) => option.text.includes(text));
//             };
//             render(
//                 <Grid>
//                     <StatefulSuggestSelect
//                         label="label"
//                         onLoad={loadOptions}
//                         defaultValue={options[0]}
//                     />
//                 </Grid>
//             );

//             const readingArea = screen.getByLabelText("reading area");
//             const label = getByLabelText(readingArea, "label");
//             userEvent.click(label);
//         });

//         afterEach(() => {
//             cleanup();
//         });

//         it("hides reading area", () => {
//             const readingArea = screen.getByLabelText("reading area");

//             expect(readingArea).not.toBeVisible();
//         });

//         it("shows options", () => {
//             const testOption2 = screen.getByText("test option 2");
//             expect(testOption2).toBeVisible();
//         });

//         it("loads options on text change", async () => {
//             expect(screen.getByText("test option 3")).toBeInTheDocument();

//             const textbox = screen.getByRole("textbox");
//             userEvent.type(textbox, "test option 2");

//             await waitFor(() => {
//                 expect(
//                     screen.queryByText("test option 3")
//                 ).not.toBeInTheDocument();
//                 expect(screen.getAllByText("test option 2")).toHaveLength(2);
//             });
//         });

//         it("finishes editing on outside click", () => {
//             userEvent.click(document.body);

//             const testOption2 = screen.getByText("test option 2");
//             expect(testOption2).not.toBeVisible();
//         });
//     });

//     describe("after click on option", () => {
//         beforeEach(() => {
//             const options = [
//                 { id: 1, text: "test option 1" },
//                 { id: 2, text: "test option 2" },
//             ];
//             const loadOptions = () => options;
//             render(
//                 <Grid>
//                     <StatefulSuggestSelect
//                         label="label"
//                         onLoad={loadOptions}
//                         defaultValue={options[0]}
//                     />
//                 </Grid>
//             );

//             const readingArea = screen.getByLabelText("reading area");
//             const label = getByLabelText(readingArea, "label");
//             userEvent.click(label);

//             userEvent.click(screen.getByText("test option 2"));
//         });

//         afterEach(() => {
//             cleanup();
//         });

//         it("renders new value", () => {
//             const readingArea = screen.getByLabelText("reading area");
//             const value = getByLabelText(readingArea, "value");
//             expect(value).toHaveTextContent("test option 2");
//         });
//     });

//     describe("keyboard navigation", () => {
//         beforeAll(() => {
//             const load = () => [
//                 {
//                     id: 1,
//                     text: "test option 1",
//                 },
//                 {
//                     id: 2,
//                     text: "test option 2",
//                 },
//             ];

//             render(
//                 <Grid>
//                     <StatefulSuggestSelect
//                         label="label"
//                         onLoad={load}
//                         defaultValue={null}
//                     />
//                     <StatefulSuggestSelect
//                         label="label"
//                         onLoad={load}
//                         defaultValue={null}
//                     />
//                     <StatefulSuggestSelect
//                         label="label"
//                         onLoad={load}
//                         defaultValue={null}
//                     />
//                 </Grid>
//             );

//             userEvent.tab();
//             userEvent.keyboard("{enter}");
//         });

//         it("focuses self with tab", async () => {
//             const readingAreas = screen.getAllByLabelText("reading area");
//             expect(readingAreas[0]).toHaveFocus();

//             userEvent.tab();
//             expect(readingAreas[1]).toHaveFocus();

//             userEvent.tab();
//             expect(readingAreas[2]).toHaveFocus();

//             userEvent.tab();
//             expect(readingAreas[0]).toHaveFocus();

//             userEvent.tab({ shift: true });
//             expect(readingAreas[2]).toHaveFocus();

//             userEvent.tab({ shift: true });
//             expect(readingAreas[1]).toHaveFocus();

//             userEvent.tab({ shift: true });
//             expect(readingAreas[0]).toHaveFocus();
//         });

//         it("shows options after enter", () => {
//             userEvent.keyboard("{enter}");

//             const readingAreas = screen.getAllByLabelText("reading area");

//             expect(readingAreas[0]).not.toBeVisible();
//             expect(screen.getByText("test option 1")).toBeInTheDocument();
//         });

//         it("changes with second option after arrowDown and enter", async () => {
//             userEvent.keyboard("{arrowdown}{enter}");

//             const readingAreas = screen.getAllByLabelText("reading area");

//             expect(readingAreas[0]).toBeVisible();
//             const value = getByLabelText(readingAreas[0], "value");
//             expect(value).toHaveTextContent("test option 2");
//         });
//     });
// });
