import { OnboardingService } from '../Onboarding';
import { usersMockFactory } from '../../store/__tests__/mock/user';

class LocalStorageMock {
    private store = {};

    public clear() {
        this.store = {};
    }

    public getItem(key) {
        return this.store[key] || null;
    }

    public setItem(key, value) {
        this.store[key] = value.toString();
    }

    public removeItem(key) {
        delete this.store[key];
    }
}

(global as any).localStorage = new LocalStorageMock();
const ONBOARDING_KEY = 'mssngr:corporate_onboarding';
let onboarding: OnboardingService;

(window as any).flags = { onboarding: '1', enableWorkplace: '1' };
(FLAGS as any) = { MESSENGER: true };

describe('Onboarding service', () => {
    describe('completeStep', () => {
        beforeEach(() => {
            onboarding = new OnboardingService();
            localStorage.clear();
        });

        it('Добавляет в локал стор верную запись', () => {
            [0].forEach((key) => {
                if (key > 0) {
                    expect(localStorage.getItem(ONBOARDING_KEY)).toBe(`${key}`);
                } else {
                    expect(localStorage.getItem(ONBOARDING_KEY)).toBe(null);
                }
                onboarding.completeStep();
                expect(localStorage.getItem(ONBOARDING_KEY)).toBe(`${key + 1}`);
            });
        });
    });

    describe('getCurrentStep', () => {
        const usersMock = usersMockFactory();

        beforeEach(() => {
            localStorage.clear();
            onboarding = new OnboardingService();
        });

        it('Не возвращает шаг если не передан пользователь', () => {
            expect(onboarding.getCurrentStep()).toBe(-1);
        });

        it('Сценарий пользователя состоящего в организации', () => {
            onboarding.updateUser(usersMock.createOrganizationUser()()[0]);

            expect(onboarding.getCurrentStep()).toBe(0);

            onboarding.completeStep();

            expect(onboarding.getCurrentStep()).toBe(-1);
        });

        it('Сценарий пользователя не состоящего в организации', () => {
            onboarding.updateUser(usersMock.createUnlimited()()[0]);

            expect(onboarding.getCurrentStep()).toBe(-1);
        });
    });
});
