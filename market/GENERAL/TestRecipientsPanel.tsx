import React, {useEffect, useState} from 'react';
import {
    Button,
    ButtonGroup,
    ButtonToolbar,
    Col,
    Form,
    FormControl,
    FormGroup,
    Glyphicon,
    HelpBlock,
    InputGroup,
    Nav,
    NavItem,
    Row,
    Tab,
} from 'react-bootstrap';
import * as _ from 'lodash';
import cxs from 'cxs';

import MetaModalForm from 'components/form/MetaModalForm';
import {INPUT_TYPES} from 'components/form/inputs/TypesReestr';
import * as U from 'common';

import 'components/TestRecipients.css';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPlus} from '@fortawesome/free-solid-svg-icons';

const ERROR_MSG = cxs({
    color: 'red',
});

interface TestRecipientsPanelState {
    selectedTab?: any;
    showAddGroupModal?: boolean;
}

interface TestRecipientsPanelProps {
    onChange: (newGroups) => void;
    onRefresh?: () => void;
    renderItem: (item) => void;
    newValueInput?: any;
    elementsEqual?: (elem1, elem2) => void;
    validate?: (selector) => boolean;
    canAddNewValue?: boolean;
    canRefresh?: boolean;
    groups?: any[];
}

const TestRecipientsPanel: React.FC<TestRecipientsPanelProps> = props => {
    const NEW_GROUP_KEY = 'new-group';

    const selectDefaultTab = groups => {
        if (!groups) {
            return null;
        }

        return groups.length === 0 ? undefined : groups[0].id;
    };

    const [state, setState] = useState<TestRecipientsPanelState>({});

    useEffect(() => {
        const {groups} = props;

        const hasSelectedGroup =
            groups && groups.some(x => x.id === state.selectedTab);

        if (!hasSelectedGroup) {
            setState(prevState => ({
                ...prevState,
                selectedTab: selectDefaultTab(groups),
            }));
        }
    }, [props, state.selectedTab]);

    const onSelectTab = key => {
        if (key === NEW_GROUP_KEY) {
            setState(prevState => ({...prevState, showAddGroupModal: true}));
        } else {
            setState(prevState => ({...prevState, selectedTab: key}));
        }
    };

    const onGroupChange = group => {
        const newGroups = props.groups.map(x =>
            x.id === group.id ? group : x
        );

        props.onChange(newGroups);
    };

    const onGroupDelete = group => {
        const newGroups = props.groups.filter(x => x.id !== group.id);

        props.onChange(newGroups);

        setState(prevState => ({
            ...prevState,
            selectedTab: selectDefaultTab(newGroups),
        }));
    };

    const hideAddGroupModal = () =>
        setState(prevState => ({...prevState, showAddGroupModal: false}));

    const addGroup = group => {
        group.id = U.randomId();

        props.onChange([...props.groups, group]);

        setState(prevState => ({...prevState, showAddGroupModal: false}));
    };

    const {groups} = props;

    return (
        <div className="rec-panel">
            <Tab.Container
                id="test-items-tabs"
                activeKey={state.selectedTab}
                animation={false}
                onSelect={onSelectTab}
            >
                <Row className="clearfix">
                    <Col md={12}>
                        <Nav bsStyle="tabs">
                            {groups &&
                                groups.map(group => (
                                    <NavItem eventKey={group.id}>
                                        {group.name}
                                    </NavItem>
                                ))}
                            <NavItem
                                eventKey={NEW_GROUP_KEY}
                                title="Добавить группу"
                            >
                                <FontAwesomeIcon icon={faPlus} />
                            </NavItem>
                        </Nav>
                    </Col>
                    <Col md={12}>
                        <Tab.Content animation={false}>
                            {groups &&
                                groups.map(group => (
                                    <Tab.Pane eventKey={group.id}>
                                        <GroupTabContent
                                            group={group}
                                            newValueInput={props.newValueInput}
                                            elementsEqual={props.elementsEqual}
                                            renderItem={props.renderItem}
                                            validate={props.validate}
                                            onChange={onGroupChange}
                                            onDelete={() =>
                                                onGroupDelete(group)
                                            }
                                            onRefresh={props.onRefresh}
                                        />
                                    </Tab.Pane>
                                ))}
                        </Tab.Content>
                    </Col>
                </Row>
            </Tab.Container>

            <GroupModalForm
                show={state.showAddGroupModal}
                title="Добавление группы"
                onApply={addGroup}
                onHide={hideAddGroupModal}
            />
        </div>
    );
};

interface GroupTabContentState {
    newValue?: any;
    validationState?: string;
    validationMessage?: string;
    showEditGroupModal?: boolean;
}

interface GroupTabContentProps {
    elementsEqual: (item, newValue) => any;
    renderItem: (item) => void;
    group: any;
    validate?: (newValue) => boolean;
    onChange: (newGroup) => void;
    onDelete: () => void;
    onRefresh: () => void;
    newValueInput: any;
}

const GroupTabContent: React.FC<GroupTabContentProps> = props => {
    const [state, setState] = useState<GroupTabContentState>({
        newValue: {},
    });

    const changeSelection = (selectFn, item?) => {
        const {group} = props;

        const newGroup = U.copyOnWrite(group, {
            items: group.items.map(it =>
                !item || it === item
                    ? U.copyOnWrite(it, {
                          selected: selectFn(it),
                      })
                    : it
            ),
        });

        props.onChange(newGroup);
    };

    const onCheck = () => changeSelection(() => true);

    const onUncheck = () => changeSelection(() => false);

    const onInvert = () => changeSelection(x => !x.selected);

    const onNewValueChanged = newValue => {
        const isUnique = isUniqueFn(newValue);

        setState(prevState => ({
            ...prevState,
            newValue,
            validationState:
                isUnique && props.validate(newValue) ? 'success' : 'error',
            validationMessage: isUnique
                ? undefined
                : 'Данный элемент уже есть в списке',
        }));
    };

    const isUniqueFn = newValue =>
        !newValue ||
        !_.find(props.group.items, item => props.elementsEqual(item, newValue));

    const onSelectionChange = item => changeSelection(x => !x.selected, item);

    const removeItem = item => {
        const newGroup = U.copyOnWrite(props.group, {
            items: props.group.items.filter(it => it !== item),
        });

        props.onChange(newGroup);
    };

    const onAdd = () => {
        const {newValue} = state;

        newValue.selected = true;

        const {items} = props.group;

        const newGroup = U.copyOnWrite(props.group, {
            items: items ? [...items, newValue] : [newValue],
        });

        props.onChange(newGroup);

        setState(prevState => ({
            ...prevState,
            newValue: undefined,
            validationState: undefined,
            validationMessage: undefined,
        }));
    };

    const onEditButtonClick = () =>
        setState(prevState => ({...prevState, showEditGroupModal: true}));

    const hideEditGroupModal = () =>
        setState(prevState => ({...prevState, showEditGroupModal: false}));

    const editGroup = group => {
        props.onChange(group);
        hideEditGroupModal();
    };

    const NewValueInput = props.newValueInput;
    const {items} = props.group;

    return (
        <div>
            <ButtonToolbar className="rec-tab__toolbar text-right">
                <ButtonGroup>
                    <Button
                        bsSize="sm"
                        onClick={props.onRefresh}
                        title="Обновить"
                    >
                        <Glyphicon glyph="refresh" />
                    </Button>
                    <Button bsSize="sm" onClick={onCheck} title="Выбрать все">
                        <Glyphicon glyph="check" />
                    </Button>
                    <Button
                        bsSize="sm"
                        onClick={onUncheck}
                        title="Сбросить все"
                    >
                        <Glyphicon glyph="unchecked" />
                    </Button>
                    <Button
                        bsSize="sm"
                        onClick={onInvert}
                        title="Инвертировать выбор"
                    >
                        <Glyphicon glyph="adjust" />
                    </Button>
                </ButtonGroup>
                <ButtonGroup>
                    <Button
                        bsSize="sm"
                        onClick={onEditButtonClick}
                        title="Редактировать группу"
                    >
                        <Glyphicon glyph="pencil" />
                    </Button>
                    <Button
                        bsSize="sm"
                        onClick={props.onDelete}
                        title="Удалить группу"
                    >
                        <Glyphicon glyph="remove" />
                    </Button>
                </ButtonGroup>
            </ButtonToolbar>

            <div className="pre-scrollable rec-tab__list-holder">
                {items &&
                    items.map((item, i) => (
                        <div key={i}>
                            <InputGroup>
                                <InputGroup.Addon>
                                    <input
                                        type="checkbox"
                                        aria-label="..."
                                        checked={item.selected}
                                        onChange={() => onSelectionChange(item)}
                                    />
                                </InputGroup.Addon>
                                <FormControl
                                    value={props.renderItem(item)}
                                    disabled={true}
                                />
                                <InputGroup.Button>
                                    <Button onClick={() => removeItem(item)}>
                                        <Glyphicon glyph="remove" />
                                    </Button>
                                </InputGroup.Button>
                            </InputGroup>
                        </div>
                    ))}
            </div>

            <Form>
                <FormGroup>
                    <InputGroup>
                        <NewValueInput
                            value={state.newValue}
                            onChange={onNewValueChanged}
                        />
                        <InputGroup.Button>
                            <Button
                                disabled={
                                    !state.newValue ||
                                    state.validationState !== 'success'
                                }
                                onClick={onAdd}
                            >
                                <FontAwesomeIcon icon={faPlus} />
                            </Button>
                        </InputGroup.Button>
                    </InputGroup>
                    <HelpBlock bsClass={ERROR_MSG}>
                        {state.validationMessage}
                    </HelpBlock>
                </FormGroup>
            </Form>

            <GroupModalForm
                show={state.showEditGroupModal}
                title="Редактирование группы"
                value={props.group}
                onApply={editGroup}
                onHide={hideEditGroupModal}
            />
        </div>
    );
};

GroupTabContent.defaultProps = {
    validate: () => true,
};

TestRecipientsPanel.defaultProps = {
    canAddNewValue: true,
    canRefresh: true,
    validate: () => true,
};

export default TestRecipientsPanel;

const GROUP_MODAL_META = {
    name: {
        type: INPUT_TYPES.STRING,
        title: 'Название',
        required: true,
    },
};

const GroupModalForm = props => (
    <MetaModalForm meta={GROUP_MODAL_META} {...props} />
);
