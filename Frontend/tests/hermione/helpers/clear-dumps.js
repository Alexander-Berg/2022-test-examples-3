const fs = require('fs');
const glob = require('glob');
const path = require('path');

const root = path.resolve(__dirname, '../suites');

function globAsPromise(path, options) {
    return new Promise((resolve, reject) => {
        glob(path, options, (err, files)=>{
            if (err) {
                console.error(err);
                reject(err);
            }

            resolve(files);
        });
    });
}

async function clearDumps() {
    const files = await globAsPromise('**/*.json.gz', { cwd: root });
    files.forEach(file => fs.unlink(path.resolve(root, file), () => null));
    // eslint-disable-next-line no-console
    console.log(files);
}

clearDumps();
