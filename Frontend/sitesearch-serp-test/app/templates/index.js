const fs = require('fs');
const path = require('path');
const FILES = new Map();

const loadTemplate = (templateName, callback) => {
    if (! /\.html$/.test(templateName)) {
        return callback({ code: 404 })
    }

    if (FILES.has(templateName)) {
        callback(null, FILES.get(templateName))
    } else {
        fs.readFile(path.resolve(__dirname, templateName), (err, data) => {
            if (err) {
                console.error(err);
                return callback({
                    code: err.code === 'ENOENT' ? 404 : 500
                });
            }

            callback(null, data.toString());
        })
    }
}

const template = (templateName, data, callback) => {
    loadTemplate(templateName, (err, html) => {
        if (err) {
            return callback(err)
        }

        const { staticHost, serp, serpStatic, staticUrl } = data;
        const ya_site_path = JSON.stringify({ staticHost, serp, serpStatic });

        const script = `<script>
            var ya_site_path = ya_site_path || {}; ya_site_path = ${ ya_site_path };
            var staticUrl = "${staticUrl}";
        </script>`

        console.log('script', script);

        callback(null, html.replace('<!-- ya_site_path -->', script));
    })
}

module.exports.template = template;