import boundMethod from 'autobind-decorator';
import concat from 'lodash/concat';
import groupBy from 'lodash/groupBy';
import React, { ChangeEvent, Component } from 'react';

import { IAuthorSolution, VerdictShortName } from 'common/types/problem';
import { IGroupedCompilers } from 'common/types/compiler';

import Button from 'client/components/button';
import ConfirmationModal from 'client/components/confirmation-modal';
import IconControl from 'client/components/icon-control';
import { defaultCompilerId, defaultVerdict } from 'client/components/problem-settings/defaults';
import SelectFileModal from 'client/components/problem-settings/select-file-modal';
import { Props } from 'client/components/problem-tests/author-solutions/types';
import Select from 'client/components/select';
import Table from 'client/components/table';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-tests/author-solutions/author-solutions.css';

const b = block('author-solutions');

class AuthorSolutions extends Component<Props> {
    private readonly selectMaxHeight = 200;

    private get compilersByLangOptions() {
        const { compilers } = this.props;

        const grouped = (compilers ? groupBy(compilers, 'lang') : []) as IGroupedCompilers;

        return Object.keys(grouped).map((title) => {
            const comps = grouped[title];
            const items = comps.map(({ id, name }) => ({
                value: id,
                content: name,
            }));

            return { title, items };
        });
    }

    public componentDidMount() {
        const {
            fetchCompilersError,
            fetchCompilersStarted,
            compilers,
            fetchCompilers,
        } = this.props;

        if (!compilers && !fetchCompilersError && !fetchCompilersStarted) {
            fetchCompilers();
        }
    }

    public componentWillUnmount() {
        const { submitSolutionStarted, clearSubmissionStatus, problemId } = this.props;
        if (submitSolutionStarted) {
            clearSubmissionStatus({ problemId });
        }
    }

    public render() {
        const { className, readonly } = this.props;

        return (
            <section className={b({}, [className])}>
                <h2 className={b('title')}>
                    {i18n.text({ keyset: 'problem-settings', key: 'solutions' })}
                </h2>
                <div className={b('controls')}>
                    {!readonly && this.renderControlButtons()}
                    {this.renderTable()}
                </div>
            </section>
        );
    }

    private get isControlsDisabled() {
        const {
            submitSolutionStarted,
            settingsFetchStarted,
            settingsSubmitStarted,
            readonly,
        } = this.props;

        return submitSolutionStarted || settingsFetchStarted || settingsSubmitStarted || readonly;
    }

    private renderControlButtons() {
        const { problemId } = this.props;

        return (
            <div className={b('control-buttons')}>
                <SelectFileModal
                    title={i18n.text({ keyset: 'problem-settings', key: 'add-solution' })}
                    openButtonText={i18n.text({ keyset: 'problem-settings', key: 'add-solution' })}
                    problemId={problemId}
                    onSubmit={this.onSolutionAdd}
                />
            </div>
        );
    }

    @boundMethod
    private onSolutionAdd(files: string[]) {
        const { solutions } = this.props;

        const formatted = files.map((file) => ({
            compilerId: defaultCompilerId,
            sourcePath: file,
            verdict: defaultVerdict,
        }));

        this.saveSolutions(concat(formatted, solutions));
    }

    private renderTable() {
        const { solutions } = this.props;

        return (
            <Table
                data={solutions}
                className={b('table')}
                columns={this.getTableColumns()}
                fallbackText={i18n.text({ keyset: 'problem-settings', key: 'no-author-solutions' })}
            />
        );
    }

    private getTableColumns() {
        const { readonly } = this.props;

        return [
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'source-path' }),
                render: ({ sourcePath }: IAuthorSolution) => sourcePath,
                className: b('source-path'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'compiler' }),
                render: (solution: IAuthorSolution) => (
                    <Select
                        size="m"
                        theme="normal"
                        value={solution.compilerId}
                        disabled={this.isControlsDisabled}
                        onChange={this.onCompilerChange(solution)}
                        maxHeight={this.selectMaxHeight}
                        options={this.compilersByLangOptions}
                    />
                ),
                className: b('compiler'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'verdict' }),
                render: (solution: IAuthorSolution) => (
                    <Select
                        size="m"
                        theme="normal"
                        value={solution.verdict}
                        disabled={this.isControlsDisabled}
                        onChange={this.onVerdictChange(solution)}
                        maxHeight={this.selectMaxHeight}
                        options={Object.entries(VerdictShortName).map(([name, value]) => ({
                            value,
                            content: name,
                        }))}
                    />
                ),
                className: b('verdict'),
            },
            {
                render: (solution: IAuthorSolution) => {
                    if (readonly) {
                        return null;
                    }

                    return (
                        <Button
                            size="m"
                            theme="normal"
                            disabled={this.isControlsDisabled}
                            onClick={this.onSubmitSolution(solution)}
                        >
                            {i18n.text({ keyset: 'problem-settings', key: 'submit-solution' })}
                        </Button>
                    );
                },
                className: b('submit-solution'),
            },
            {
                render: ({ sourcePath }: IAuthorSolution) => {
                    if (readonly) {
                        return null;
                    }

                    return (
                        <ConfirmationModal
                            title={i18n.text({
                                keyset: 'common',
                                key: 'remove-this',
                                params: { name: sourcePath },
                            })}
                            confirmText={i18n.text({ keyset: 'common', key: 'delete' })}
                            onConfirm={this.onDeleteSolution(sourcePath)}
                        >
                            {(openModal) => (
                                <IconControl
                                    type="close-16"
                                    size="m"
                                    disabled={this.isControlsDisabled}
                                    onClick={openModal}
                                />
                            )}
                        </ConfirmationModal>
                    );
                },
                className: b('delete-icon'),
            },
        ];
    }

    private updateSolution(
        solution: IAuthorSolution,
        field: string,
        value: IAuthorSolution['compilerId'] | IAuthorSolution['verdict'],
    ) {
        const savedSolution = this.props.solutions.find(
            (item) => item.sourcePath === solution.sourcePath,
        );

        if (!savedSolution) {
            return;
        }

        savedSolution[field] = value;
        solution[field] = value;
    }

    private onCompilerChange(solution: IAuthorSolution) {
        const { solutions } = this.props;

        return (event: ChangeEvent<HTMLSelectElement>) => {
            const compilerId = event.target.value;
            this.updateSolution(solution, 'compilerId', compilerId);
            this.saveSolutions(solutions);
        };
    }

    private onVerdictChange(solution: IAuthorSolution) {
        const { solutions } = this.props;

        return (event: ChangeEvent<HTMLSelectElement>) => {
            const verdict = event.target.value as VerdictShortName;
            this.updateSolution(solution, 'verdict', verdict);
            this.saveSolutions(solutions);
        };
    }

    private onDeleteSolution(deletingSourcePath: string) {
        const { solutions } = this.props;

        return () => {
            const filtered = solutions.filter(
                ({ sourcePath }) => deletingSourcePath !== sourcePath,
            );

            this.saveSolutions(filtered);
        };
    }

    private onSubmitSolution(solution: IAuthorSolution) {
        const { submitSolution, problemId } = this.props;
        const { compilerId, sourcePath: filePath } = solution;

        return () => {
            submitSolution({ problemId, compilerId, filePath });
        };
    }

    private saveSolutions(solutions: IAuthorSolution[]) {
        const { submitSettings, problemId } = this.props;

        submitSettings({ problemId, settings: { solutions } });
    }
}

export default AuthorSolutions;
