module.exports = {
    async generateSimpleHtml() {
        return `<!DOCTYPE html>
            <html lang="ru">
                <head>
                    <title>..</title>
                    <meta charset="UTF-8">
                    <script>console.log(0)</script>
                    <style>body{ margin: 0}</style>               
                </head>
                    {{DYNAMIC_CONTENT}}
            </html>`;
    },
};
