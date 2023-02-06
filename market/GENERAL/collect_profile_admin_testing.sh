#!/usr/bin/env bash

echo '<html><head>    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>    <script>        $(function () {            var stats = {};            $("tr").each(function (index, it) {                var method = $($(it).children("td")[1]).text();                if (method != "" && method != "com.jamonapi.Exceptions, Exception") {                    if (stats[method] == undefined) {                        stats[method] = {                            total: 0,                            hits: 0,                            min: 9999999999,                            max: -1                        };                    }                    stats[method].hits += parseFloat($($(it).children("td")[2]).text());                    stats[method].total += parseFloat($($(it).children("td")[4]).text());                    stats[method].min = Math.min(stats[method].min, parseFloat($($(it).children("td")[7]).text()));                    stats[method].max = Math.max(stats[method].max, parseFloat($($(it).children("td")[8]).text()));                }            });            var totalTable = $("#total");            for (var method in stats) {                if (stats.hasOwnProperty(method)) {                    var stat = stats[method];                    totalTable.append("<tr>" +                        "<td>" + method + "</td>" +                        "<td>" + stat.hits + "</td>" +                        "<td>" + Math.round(stat.total / stat.hits * 1000) / 1000 + "</td>" +                        "<td>" + stat.total + "</td>" +                        "<td>" + stat.min + "</td>" +                        "<td>" + stat.max + "</td>" +                        "</tr>");                }            }        })    </script></head><body><table border="1" rules="all" id="total">    <th>Label</th><th>Hits</th><th>Avg</th><th>Total</th><th>Min</th><th>Max</th></table>'

echo "<h3>sas1-2414.search.yandex.net</h3>"
echo `curl sas1-2414.search.yandex.net:14328/profiling`

echo "<h3>sas1-2415.search.yandex.net</h3>"
echo `curl sas1-2415.search.yandex.net:14328/profiling`

echo '</body></html>'
