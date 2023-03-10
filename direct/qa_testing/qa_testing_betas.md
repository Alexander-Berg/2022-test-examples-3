# Беты директа 

## Что такое бета

Бета - экземпляр (среда/инстанс) Директа для разработки и тестирования. Беты запускаются на **ppcdev**, состоят из рабочей копии и нескольких процессов, запущенных над нею. Беты доступны по адресам вида https://8080.beta1.direct.yandex.ru, где 8080 — номер беты (он же порт).
________________________________________________________________________________________________________________________________________
## Какие бывают беты

Существует несколько "перманентных" бет (их еще называют общими, или тестовыми, или главными):
- https://8080.beta1.direct.yandex.ru - прод код, dev-test база
- https://8998.beta1.direct.yandex.ru - trunk(master), dev-test база
- https://8999.beta1.direct.yandex.ru - релизный код, dev-test база

Отдельно можно выделить **ТС**
- https://test.direct.yandex.ru - релизный код, ТС база

Помимо основных бет, также можно создать свою или воспользоваться той, которую предоставил разработчик.
На данный момент, пользовательские беты можно разделить на два типа:  **ARC** и **SVN** - в зависимости от инструмента ci/cd, используемого для её создания.

Пример урла такой беты:
- https://14350.beta7.direct.yandex.ru -  код задачи + dev-test/ТС база
________________________________________________________________________________________________________________________________________
## Как понять, какая бета мне нужна?

Выбор типа беты, а также способ её создания, в большинстве своём, зависит от репозитория ветки задачи.
В задачах чаще всего встречаются следующие репозитории: **DNA**, **JavaWeb**, **Perl**

Увидеть репозиторий, в котором выполнена задача, можно в поле **Affected apps**:

![](https://jing.yandex-team.ru/files/ivanlog/2021-11-11T14:35:23Z.9d5251b.png)

{% note warning %}

Для **DNA** и **JavaWeb** всегда используется **ARC**

Для **Perl** может быть как **SVN**, так и **ARC** - на выбор команды.

{% endnote %}

Понять, какой из ci/cd сервисов использовался для "перловой" задачи можно перейдя в **Arcanum**.
Как правило, ссылка на бранчу в аркануме прикреплена к тикету и её легко можно распознать по иконке ![](https://proxy.sandbox.yandex-team.ru/415539274) напротив неё.

![](https://jing.yandex-team.ru/files/ivanlog/2021-11-11T14:45:49Z.32ddb14.png)

Перейдя в **Arcanum**, мы сможем увидеть SVN или же ARC бета нам попалась:
![](https://jing.yandex-team.ru/files/ivanlog/2021-11-11T14:48:59Z.702aebb.png)

________________________________________________________________________________________________________________________________________
## Создание бет

Как мы уже выяснили ранее, способ создания беты зависит от репозитория разработки. Если в задаче затронут только один репозиторий - достаточно выполнить одну из команд представленных ниже.

{% note alert %}

Перед тем как выполнять какие-либо команды, необходимо подключиться к **ppcdev** по **ssh**. Для этого понадобится добавить ключ и запросить доступ, если это ещё не сделано. Инструкции можно найти [тут](https://docs.yandex-team.ru/direct-dev/guide/qa/qa_kmb/qa_first_steps#dostup-k-testovym-stendam). Затем требуется зарезервировать под себя порт [резервирование портов](https://docs.yandex-team.ru/direct-dev/dev/betas/betas#beta-ports)

Пример команды: `ssh -A ppcdev7.yandex.ru`
Всего существует 26 ppcdev.


{% endnote %}
##### Список переменных используемых в инструкции и где их взять:
{% cut "Открывающийся список" %}

Переменная | Значение
:--- | :---
`<username>` | Имя (твоего) пользователя
`<port_number>` | Номер порта/беты
`<branch_name>` | Имя ветки. Можно скопировать из **Arcanum** ![](https://jing.yandex-team.ru/files/ivanlog/2021-11-11T15:07:12Z.5888bf1.png)
`<commit>` | Номер коммита. Также можно найти в **Arcanum** в урле ![](https://jing.yandex-team.ru/files/ivanlog/2021-11-11T15:10:00Z.5bb7fb2.png)


{% endcut %}


### DNA

```
direct-create-beta --dna branch:<branch_name>
```
### Java-web
```
direct-create-beta --java-svn app:web,rp:<commit>
```

### Perl
#### SVN
```
direct-create-beta -b <branch_name>
```
Пример тикета: https://st.yandex-team.ru/DIRECT-155497

#### ARC
```
mkdir /var/www/beta.`whoami`.`direct-create-beta --get-port-only`
cd $_
mkdir -p arcadia store
arc mount -m arcadia/ -S store/
cd arcadia
arc fetch <branch_name>
arc checkout -b mybranch <branch_name>
direct-mk beta-postcreate
```
Пример тикета: https://st.yandex-team.ru/DIRECT-154003

### Создание бет с несколькими репозиториями одновременно

Если нам нужно создать бету, на которой разные репозитории будут смотреть на разные ветки, мы можем это сделать используя команду подобного вида:

```
direct-create-beta -b <branch_name> --java-svn app:web,rp:<commit> --dna branch:<branch_name>
```
{% cut "тык" %}

![](https://jing.yandex-team.ru/files/ivanlog/maxresdefault.918ac05.png)

{% endcut %}

Пример: `direct-create-beta -b DIRECT-154733 --java-svn app:web,rp:2064933 --dna branch:users/spt30/DIRECT-153512`

Пример: `direct-create-beta --java-svn app:web,rp:2063130 --dna branch:trunk`



#### Дополнительные параметры:

К команде сборке беты могут быть добавлены дополнительные параметры. Ниже приведены наиболее частоиспользуемые команды.

`no-sandbox:1` - собирает не в сендбоксе - повышает стабильность, но замедляет сборку

Пример: `direct-create-beta --java-svn app:web,no-sandbox:1,rp:2083724`
________________________________________________________________________________________________________________________________________
## Удаление бет

### SVN
```
direct-delete-beta -f /var/www/beta.<username>.<port_number>
```

### ARC
```
kill $( ps aux | grep beta.<username>.<port_number> | awk '{print $2}')
rm -rf /var/www/beta.<username>.<port_number>
```
________________________________________________________________________________________________________________________________________
## Другие операции с бетами
Посмотреть список всех бет:
```
betas
```
Посмотреть список бет конкретного юзера:
```
betas <username>
```
Повернуть бету на ТС
```
b <port_number>
direct-mk conf-test
```
________________________________________________________________________________________________________________________________________
--
<br>
В случае неактуальности информации на данной странице можно обращаться к [Ивану Логинову](https://staff.yandex-team.ru/ivanlog)
