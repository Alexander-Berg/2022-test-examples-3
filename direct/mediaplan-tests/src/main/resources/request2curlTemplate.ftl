<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <link rel="stylesheet" href="css/app.css"/>
    <link rel="stylesheet" href="https://yandex.st/bootstrap/3.1.1/css/bootstrap.min.css"/>
    <script src="https://yandex.st/jquery/2.0.3/jquery.min.js"></script>
    <script>
        function getCurl(){
            $('#curl').show();
            $('#usualFormat').hide();
            $('#showCurl').hide();
            $('#showUsualFormat').show();
        }
        function getUsualFormat(){
            $('#curl').hide();
            $('#usualFormat').show();
            $('#showCurl').show();
            $('#showUsualFormat').hide();
        }
    </script>
</head>
<body>
<div class="container">
    <p>Show format:
        <button id="showCurl" onClick="getCurl()">CURL</button>
        <button style="display:none" id="showUsualFormat" onClick="getUsualFormat()">${buttonName}</button>
        </p>
    <pre id="usualFormat"><code>${usualFormatString?html}</code></pre>
    <pre id="curl" style="display:none"><code>${curlString?html}</code></pre>
</div>
</body>
</html>