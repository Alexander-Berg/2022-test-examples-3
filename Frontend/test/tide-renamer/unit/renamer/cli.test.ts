import 'mocha';
import sinon from 'sinon';
import { expect } from 'chai';
import inquirer from 'inquirer';
import type { Test } from '../../../../src';
import { confirmationPrompt } from '../../../../src/plugins/tide-renamer/renamer/cli';
import { Tide } from '../../../../src';

const tide = {
    config: {
        silent: false,
    },
} as Tide;

describe('tide-renamer / renamer / cli', () => {
    describe('confirmationPrompt', () => {
        it('should log changed files and return true if the prompt was confirmed', async () => {
            sinon.stub(inquirer, 'prompt').resolves({ continue: true });
            const consoleLogStub = sinon.stub(console, 'log');
            const tests = [
                {
                    tools: new Set(['hermione']),
                    filePaths: { hermione: 'hermione-path-1' },
                },
                {
                    tools: new Set(['testpalm']),
                    filePaths: { testpalm: 'testpalm-path-1' },
                },
                {
                    tools: new Set(['hermione']),
                    filePaths: { hermione: 'hermione-path-1' },
                },
                {
                    tools: new Set(['hermione']),
                    filePaths: { hermione: 'hermione-path-2' },
                },
            ];
            let lines = [
                'The following files will be affected (apart from assets and metrics.json files):',
                '- hermione-path-1',
                '- testpalm-path-1',
                '- hermione-path-2',
            ];

            const confirmationPromptResult = await confirmationPrompt(
                tests as unknown as Array<Test>,
                tide,
            );

            expect(confirmationPromptResult).equal(true);
            lines.forEach((line) => {
                expect(consoleLogStub.calledWith(line)).equal(true);
            });
        });
    });

    afterEach(() => {
        sinon.restore();
    });
});
