import * as chalk from 'chalk';
import { LintStatus } from './types';
import { prettifyHint } from './pretify';

describe('Pretify', () => {
    let report = {
        name: 'packageName',
        status: LintStatus.Ok,
        current: 'currentVer',
        target: 'targetVer',
    };

    it('should suggest green version', () => {
        expect(prettifyHint(report))
            .toBe(`[${chalk.green('✔')}] ${chalk.blue('packageName')}: ${chalk.green('targetVer')}`);
    });

    it('should suggest yellow version', () => {
        report = {
            ...report,
            status: LintStatus.OutdatedVersion,
        };

        expect(prettifyHint(report))
            .toBe(`[${chalk.yellow('!')}] ${chalk.blue('packageName')}: ${chalk.yellow('currentVer')} -> ${chalk.green('targetVer')}`);
    });

    it('should suggest red version', () => {
        report = {
            ...report,
            status: LintStatus.RestrictedVersion,
        };

        expect(prettifyHint(report))
            .toBe(`[${chalk.red('×')}] ${chalk.blue('packageName')}: ${chalk.red('currentVer')} -> ${chalk.green('targetVer')}`);
    });
});
