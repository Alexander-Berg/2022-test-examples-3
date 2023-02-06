{% include 'header.tpl' %}

<!-- Static navbar -->
<nav class="navbar navbar-default" role="navigation">
    <div class="container-fluid">
        <div class="navbar-header navbar-brand">
<!--            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button> -->
            On-call администратор Метрики
        </div>
{% if user in acl %}
        <div id="navbar" class="navbar-collapse collapse">
            <ul class="nav navbar-nav navbar-right">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown">Сменить дежурного <span class="caret"></span></a>
                    <ul class="dropdown-menu" role="menu">
                    {% for login in users|sort %}
                      <li><a href="/?set_duty={{ login }}">{{ login }}@</a></li>
                    {% endfor %}
                    </ul>
                </li>
            </ul>

        </div><!--/.nav-collapse -->
{% endif %}
    </div><!--/.container-fluid -->
</nav>

<div class="container">
<!-- <div class="jumbotron"> -->

    <div class="page-header">
        <h1>На этой неделе дежурит <a class="auto-person-card" href="//staff.yandex-team.ru/{{ duty['result'][0]['login'] }}" data-login="{{ duty['result'][0]['login'] }}">{{ duty['result'][0]['login'] }}@</a></h1>
    </div>
    {{ additional_message }}
    <p class="lead">Напомним как и когда мы выкладываем пакеты: <a href='https://wiki.yandex-team.ru/JandexMetrika/admin/ReleaseRules'>wiki/JandexMetrika/admin/ReleaseRules</a></p>
</div>

<div class="footer">
    <div class="container">
        <p class="text-muted"><a href="xmpp:{{ duty['result'][0]['login'] }}@yandex-team.ru">Написать</a> в jabber дежурному.</p>
    </div>
</div>
<!-- </div> -->

{% include 'footer.tpl' %}
