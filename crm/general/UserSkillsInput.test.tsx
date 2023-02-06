import React from 'react';
import { render, screen, waitFor, getByRole } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import userEvent from '@testing-library/user-event';
import { ArrayOperationBuilder } from 'utils/ArrayOperationBuilder';
import { UserSkillWithValue } from 'types/UserSkill';
import { UserSkillsInput } from './UserSkillsInput';
import { UserSkillsProviderByCallback } from './UserSkillsProviderByCallback';

const getAddSkillButton = () => screen.getByTestId('add-button');
const getSkillMenuItem = (skillName: string) => screen.getByText(skillName);
const getSaveButtonForm = () => screen.getByText(/сохранить/i);
const getSelectValueButton = () => screen.getByRole('listbox');
const getSelectValueOption = (value: number) => screen.getByRole('option', { name: String(value) });
const getActiveSkillByName = (name: string) =>
  screen.getByRole('button', { name: new RegExp(name, 'i') });

const setSkillValueInForm = async (value: number) => {
  userEvent.click(getSelectValueButton());

  await waitFor(() => getSelectValueOption(value));
  userEvent.click(getSelectValueOption(value));
};

const addSkill = async ({ name, value }: { name: string; value?: number }) => {
  userEvent.click(getAddSkillButton());

  await waitFor(() => userEvent.click(getSkillMenuItem(name)));

  if (value) {
    await setSkillValueInForm(value);
  }

  await waitFor(() => userEvent.click(getSaveButtonForm()));
};

const changeSkill = async ({ name, value }: { name: string; value: number }) => {
  userEvent.click(getActiveSkillByName(name));
  await setSkillValueInForm(value);
  await waitFor(() => userEvent.click(getSaveButtonForm()));
};

const removeSkill = async ({ name }: { name: string }) => {
  const skill = getActiveSkillByName(name);

  const removeButton = getByRole(skill, 'button', { name: 'remove' });
  userEvent.click(removeButton);
};

describe('UserSkillsInput', () => {
  describe('.value', () => {
    describe('when no set value', () => {
      it('renders placeholder', () => {
        render(<UserSkillsInput />);

        expect(screen.queryByText('Добавить навык')).toBeInTheDocument();
      });
    });

    describe('when value set to empty array', () => {
      it('renders placeholder', () => {
        render(<UserSkillsInput value={[]} />);

        expect(screen.queryByText('Добавить навык')).toBeInTheDocument();
      });
    });

    describe('when not empty array', () => {
      it('renders skills', () => {
        render(
          <UserSkillsInput
            value={[
              { id: 1, name: 'skill1', value: 1 },
              { id: 2, name: 'skill2', value: 2 },
            ]}
          />,
        );

        expect(screen.queryByText('skill1 1')).toBeInTheDocument();
        expect(screen.queryByText('skill2 2')).toBeInTheDocument();
      });
    });
  });

  describe('.onChange', () => {
    const skills = [
      { id: 1, name: 'skill1' },
      { id: 2, name: 'skill2' },
    ];

    const skillsWithValue = skills.map((skill) => ({ ...skill, value: 1 }));

    const userSkillsProviderFactory = () => new UserSkillsProviderByCallback(() => [skills]);

    const handleChange = jest.fn();

    beforeEach(() => {
      handleChange.mockClear();
    });

    describe('when add skill', () => {
      it('calls onChange', async () => {
        render(
          <TestBed>
            <UserSkillsInput
              userSkillsProviderFactory={userSkillsProviderFactory}
              onChange={handleChange}
            />
          </TestBed>,
        );

        const skillForAdd = skills[0];
        await addSkill({ name: skillForAdd.name, value: 2 });

        const arrayOperationBuilder = new ArrayOperationBuilder<UserSkillWithValue>([]);
        const addOperation = arrayOperationBuilder.addItem({ ...skillForAdd, value: 2 })!;

        expect(handleChange).toBeCalledWith(addOperation.array.next, addOperation);
      });
    });

    describe('when remove skill', () => {
      it('calls onChange', async () => {
        render(
          <TestBed>
            <UserSkillsInput
              userSkillsProviderFactory={userSkillsProviderFactory}
              onChange={handleChange}
              value={skillsWithValue}
            />
          </TestBed>,
        );

        const skillForRemove = skillsWithValue[0];
        await removeSkill({ name: skillForRemove.name });

        const arrayOperationBuilder = new ArrayOperationBuilder<UserSkillWithValue>(
          skillsWithValue,
        );
        const removeOperation = arrayOperationBuilder.removeItem(skillForRemove)!;

        expect(handleChange).toBeCalledWith(removeOperation.array.next, removeOperation);
      });
    });

    describe('when change skill', () => {
      it('calls onChange', async () => {
        render(
          <TestBed>
            <UserSkillsInput
              userSkillsProviderFactory={userSkillsProviderFactory}
              onChange={handleChange}
              value={skillsWithValue}
            />
          </TestBed>,
        );

        const skillForChange = skillsWithValue[0];
        await changeSkill({ name: skillForChange.name, value: 2 });
        const changedSkill = { ...skillForChange, value: 2 };

        const arrayOperationBuilder = new ArrayOperationBuilder<UserSkillWithValue>(
          skillsWithValue,
        );
        const changeOrAddOperation = arrayOperationBuilder.changeOrAddItem(changedSkill)!;

        expect(handleChange).toBeCalledWith(changeOrAddOperation.array.next, changeOrAddOperation);
      });
    });
  });
});
