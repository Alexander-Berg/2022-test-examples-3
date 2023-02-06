const fs = require('fs');

function removeDir(dirPath) {
    if (!fs.existsSync(dirPath)) {
        return;
    }

    const files = fs.readdirSync(dirPath);

    for (const file of files) {
        const filePath = `${dirPath}/${file}`;

        if (fs.lstatSync(filePath).isDirectory()) {
            removeDir(filePath);
        } else {
            fs.unlinkSync(filePath);
        }
    }

    fs.rmdirSync(dirPath);
}

module.exports.removeDir = removeDir;
