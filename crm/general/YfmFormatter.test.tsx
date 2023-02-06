import React from 'react';
import { render, screen } from '@testing-library/react';
import { YfmFormatter } from './YfmFormatter';
import { replaceAction } from './YfmFormatter.utils'

jest.mock('@yandex-int/magiclinks');

describe('YfmFormatter', () => {
    describe("props.markdown", () => {
        describe("when defined", () => {
            it("renders markdown", () => {
                const markdown = "markdown"
                render(
                    <YfmFormatter markdown={markdown}/>
                )

                expect(screen.getByText(markdown)).toBeInTheDocument()
            })
        })

        describe("when undefined", () => {
            it("doesn't render markdown", () => {
                const {container} = render(
                    <YfmFormatter/>
                )

                expect(container.querySelector(".yfm")?.innerHTML).toBe("")
            })
        })
    })

    describe("props.onContentUpdated", () => {
        describe("when defined", () => {
            it("calls on mount", () => {
                const onContentUpdated = jest.fn()
                render(
                    <YfmFormatter onContentUpdated={onContentUpdated}/>
                ) 

                expect(onContentUpdated).toBeCalled()
            })
        })
    })

    describe("utils.replaceAction", () => {
        it("replaces action", () => {
            const markdown = "{{myaction}}"

            const replacedMardown = replaceAction(markdown, "myaction", () => {
                return "{{replacedaction}}"
            })
    
            expect(replacedMardown).toBe("{{replacedaction}}")
        })

        it("replaces multiple actions", () => {
            const markdown = "{{myaction}} some text {{myaction}}"

            const replacedMardown = replaceAction(markdown, "myaction", () => {
                return "{{replacedaction}}"
            })
    
            expect(replacedMardown).toBe("{{replacedaction}} some text {{replacedaction}}")
        })

        it("replaces action with spaces", () => {
            const markdown = `{{  myaction   }}`

            const replacedMardown = replaceAction(markdown, "myaction", () => {
                return "{{replacedaction}}"
            })
    
            expect(replacedMardown).toBe("{{replacedaction}}")
        })

        it("doesn't replace action with similar name", () => {
            const markdown1 = `{{_myaction}}`
            const markdown2 = `{{myaction_}}`

            const replacedMardown1 = replaceAction(markdown1, "myaction", () => {
                return "{{replacedaction}}"
            })
    
            expect(replacedMardown1).toBe(markdown1)

            const replacedMardown2 = replaceAction(markdown2, "myaction", () => {
                return "{{replacedaction}}"
            })
    
            expect(replacedMardown2).toBe(markdown2)
        })

        it("gets attribute without brackets", () => {
            const markdown = `{{myaction key=value}}`

            const replaceCallback = jest.fn()
            replaceAction(markdown, "myaction", replaceCallback)
    
            expect(replaceCallback).toBeCalledWith({
                key: "value",
            })
        })

        it("gets attribute with single brackets", () => {
            const markdown = `{{myaction key='value'}}`

            const replaceCallback = jest.fn()
            replaceAction(markdown, "myaction", replaceCallback)
    
            expect(replaceCallback).toBeCalledWith({
                key: "value",
            })
        })

        it("gets attribute with double brackets", () => {
            const markdown = `{{myaction key="value"}}`

            const replaceCallback = jest.fn()
            replaceAction(markdown, "myaction", replaceCallback)
    
            expect(replaceCallback).toBeCalledWith({
                key: "value",
            })
        })

        it("gets all attributes", () => {
            const markdown = `{{myaction key1=value1 key2=value2}}`

            const replaceCallback = jest.fn()
            replaceAction(markdown, "myaction", replaceCallback)
    
            expect(replaceCallback).toBeCalledWith({
                key1: "value1",
                key2: "value2",
            })
        })

        it("gets attribute with multiple spaces", () => {
            const markdown = `{{myaction      key="value"    }}`

            const replaceCallback = jest.fn()
            replaceAction(markdown, "myaction", replaceCallback)
    
            expect(replaceCallback).toBeCalledWith({
                key: "value",
            })
        })
    })
});
