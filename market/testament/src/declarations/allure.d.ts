declare module 'allure-commandline' {
    type Generation = {
        on: (event: string, callback: (exitCode: number) => void) => void;
    };

    export default function allure(commands: Array<string>): Generation;
}
