import packageJson from '../../package.json';

function checkVersionNumbers(dependencies: Record<string, string>): void {
    let errors = '';
    Object.keys(dependencies).forEach(key => {
        const value: string = dependencies[key];
        if (value.match(/^[~^]/)) {
            errors += `You have invalid version notation in package.json: ${key}@${value}\n`;
        }
    });
    if (errors) {
        throw new Error(errors);
    }
}

describe('Validate dependencies', () => {
    it('Has no tilda and caret in version number', () => {
        expect(() => checkVersionNumbers(packageJson.dependencies)).not.toThrow();
    });
});

describe('Validate devDependencies', () => {
    it('Has no tilda and caret in version number', () => {
        expect(() => checkVersionNumbers(packageJson.devDependencies)).not.toThrow();
    });
});
