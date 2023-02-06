import React from "react";
import { render, screen } from "@testing-library/react";
import { TestBed } from "../../TestBed";
import userEvent from "@testing-library/user-event";
import { Header } from "./Header";

const deafultProps = {
    name: "name",
    onClose: jest.fn(),
};

describe("Header", () => {
    describe("props.panel", () => {
        describe("when defined", () => {
            it("renders panel.title", () => {
                const panelStub = {
                    title: "panelTitle",
                    id: "id",
                    content: "",
                };
                render(<Header {...deafultProps} panel={panelStub} />);

                expect(screen.getByText(panelStub.title)).toBeInTheDocument();
            });
        });
    });

    describe("props.hasPinButton", () => {
        describe("when true", () => {
            it("renders pin button", () => {
                render(
                    <TestBed>
                        <Header {...deafultProps} hasPinButton />
                    </TestBed>
                );

                expect(screen.getByRole("button")).toBeInTheDocument();
            });
        });

        describe("when false", () => {
            it("doesn't render pin button", () => {
                render(
                    <TestBed>
                        <Header {...deafultProps} hasPinButton={false} />
                    </TestBed>
                );

                expect(screen.queryByRole("button")).not.toBeInTheDocument();
            });
        });
    });

    describe("props.hasToggleButton", () => {
        describe("when true", () => {
            it("renders close button", () => {
                render(
                    <TestBed>
                        <Header {...deafultProps} hasToggleButton />
                    </TestBed>
                );

                expect(screen.getByRole("button")).toBeInTheDocument();
            });

            describe("props.onClose", () => {
                describe("when defined", () => {
                    it("calls on toggle click", () => {
                        const onClose = jest.fn();
                        render(
                            <TestBed>
                                <Header
                                    {...deafultProps}
                                    hasToggleButton
                                    onClose={onClose}
                                />
                            </TestBed>
                        );

                        userEvent.click(screen.getByRole("button"));
                        expect(onClose).toBeCalled();
                    });
                });
            });
        });

        describe("when false", () => {
            it("doesn't render close button", () => {
                render(
                    <TestBed>
                        <Header {...deafultProps} hasPinButton={false} />
                    </TestBed>
                );

                expect(screen.queryByRole("button")).not.toBeInTheDocument();
            });
        });
    });
});
