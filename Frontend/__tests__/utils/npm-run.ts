import { execSync } from 'child_process';

interface INpmRunOptions {
    throwError?: boolean
}

export default function npmRun(script: string, { throwError }: INpmRunOptions = {}) {
    const command = `npm run ${script}`;

    if (throwError) {
        return execSync(command);
    }

    try {
        return execSync(command).toString();
    } catch (error) {
        return error.stdout.toString();
    }
}
