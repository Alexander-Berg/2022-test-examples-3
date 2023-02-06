import { spawnSync } from 'child_process';

export const unusedPort = (): number => {
    return parseInt(
        spawnSync(process.execPath, [
            '-e',
            'require("net").createServer().listen(0,"localhost",function(){process.stdout.write(""+this.address().port);this.close()})',
        ]).stdout as unknown as string,
    );
};
