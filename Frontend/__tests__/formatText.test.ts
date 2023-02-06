import { formatText } from '../formatText';

describe('TextFormatter formatText', () => {
    describe('#formatText', () => {
        it('returns content when no tags found', () => {
            expect(formatText('Lorem ipsum dolor sit amet, consectetur adipiscing elit.'))
                .toBe('Lorem ipsum dolor sit amet, consectetur adipiscing elit.');
        });

        it('returns content without tags when markup=false', () => {
            expect(formatText('Lorem ipsum **dolor sit amet, consectetur** adipiscing elit.', false))
                .toBe('Lorem ipsum dolor sit amet, consectetur adipiscing elit.');
        });

        it('replaces stack tags', () => {
            expect(formatText('Lorem ipsum **dolor sit amet, consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor sit amet, consectetur</b> adipiscing elit.');
        });

        it('replaces regular tags', () => {
            expect(formatText('Lorem ipsum ```dolor sit amet, consectetur``` adipiscing elit.'))
                .toBe('Lorem ipsum <pre class="yamb-code"><code class="yamb-code__text text">dolor sit amet, consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum `dolor sit amet, consectetur` adipiscing elit.'))
                .toBe('Lorem ipsum <pre class="yamb-code yamb-code_inline"><code class="yamb-code__text text">dolor sit amet, consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum ```\n\ndolor sit amet,\n consectetur\n\n``` adipiscing elit.'))
                .toBe('Lorem ipsum <pre class="yamb-code"><code class="yamb-code__text text">dolor sit amet,\n consectetur</code></pre> adipiscing elit.');
        });

        it('text with markup if flag markup === true should be escaped', () => {
            expect(formatText('Lorem ipsum ```dolor sit amet, <img src="https://some.url"> consectetur``` adipiscing elit.', true))
                .toBe('Lorem ipsum <pre class=\"yamb-code\"><code class=\"yamb-code__text text\">dolor sit amet, &lt;img src=&quot;https://some.url&quot;&gt; consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum `dolor sit amet, <img src="https://some.url"> consectetur` adipiscing elit.', true))
                .toBe('Lorem ipsum <pre class=\"yamb-code yamb-code_inline\"><code class=\"yamb-code__text text\">dolor sit amet, &lt;img src=&quot;https://some.url&quot;&gt; consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum dolor sit amet, <img src="https://some.url"> consectetur adipiscing elit.', true))
                .toBe('Lorem ipsum dolor sit amet, &lt;img src=&quot;https://some.url&quot;&gt; consectetur adipiscing elit.');

            expect(formatText('Lorem ipsum dolor sit **amet, <img src="https://some.url"> consectetur** adipiscing elit.', true))
                .toBe('Lorem ipsum dolor sit <b class="text">amet, &lt;img src=&quot;https://some.url&quot;&gt; consectetur</b> adipiscing elit.');

            expect(formatText('Lorem ipsum dolor sit __amet, <img src="https://some.url"> consectetur__ adipiscing elit.', true))
                .toBe('Lorem ipsum dolor sit <i class="text">amet, &lt;img src=&quot;https://some.url&quot;&gt; consectetur</i> adipiscing elit.');
        });

        it('returns content in pre & code tag with inline classname and \\n converted to \s', () => {
            expect(formatText('`\n\nLorem ipsum dolor sit amet,\n\nconsectetur adipiscing elit.\n\n`'))
                .toBe('<pre class="yamb-code yamb-code_inline"><code class="yamb-code__text text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</code></pre>');
        });

        it('returns content in pre&code tag and \\n in start and end of code removed', () => {
            expect(formatText('```\n\ndolor sit amet,\n consectetur\n\n```'))
                .toBe('<pre class="yamb-code"><code class="yamb-code__text text">dolor sit amet,\n consectetur</code></pre>');
        });

        it('replaces mentions', () => {
            expect(formatText('Lorem ipsum @dolor sit amet, consectetur adipiscing elit.', true, { dolor: 'Dolor Tom' }))
                .toBe('Lorem ipsum <span class="link" role="link" data-guid="dolor">Dolor Tom</span> sit amet, consectetur adipiscing elit.');
        });

        it('ignores stack tags', () => {
            expect(formatText('х**ня пи**ец')).toBe('х**ня пи**ец');

            expect(formatText('Lorem ipsum **dolor sit amet, consectetur **adipiscing elit.'))
                .toBe('Lorem ipsum **dolor sit amet, consectetur **adipiscing elit.');

            expect(formatText('Lorem ipsum** dolor sit amet, consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum** dolor sit amet, consectetur** adipiscing elit.');

            expect(formatText('Lorem ipsum** dolor sit amet, consectetur **adipiscing elit.'))
                .toBe('Lorem ipsum** dolor sit amet, consectetur **adipiscing elit.');
        });

        it('replaces correct stack tags', () => {
            expect(formatText('Lorem ipsum ** dolor sit amet, consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text"> dolor sit amet, consectetur</b> adipiscing elit.');

            expect(formatText('Lorem ipsum **dolor sit amet, consectetur ** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor sit amet, consectetur </b> adipiscing elit.');

            expect(formatText('Lorem ipsum ** dolor sit amet, consectetur ** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text"> dolor sit amet, consectetur </b> adipiscing elit.');
        });

        it('replaces regular tags in stack tags', () => {
            expect(formatText('Lorem ipsum **dolor sit ```amet,``` consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor sit <pre class="yamb-code"><code class="yamb-code__text text">amet,</code></pre> consectetur</b> adipiscing elit.');

            expect(formatText('Lorem ipsum **dolor sit `amet,` consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor sit <pre class="yamb-code yamb-code_inline"><code class="yamb-code__text text">amet,</code></pre> consectetur</b> adipiscing elit.');
        });

        it('ignores tags in regular tags', () => {
            expect(formatText('Lorem ipsum ```dolor sit **amet,** consectetur``` adipiscing elit.'))
                .toBe('Lorem ipsum <pre class="yamb-code"><code class="yamb-code__text text">dolor sit **amet,** consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum ```dolor [sit amet](https://sit.amet), consectetur``` adipiscing elit.'))
                .toBe('Lorem ipsum <pre class="yamb-code"><code class="yamb-code__text text">dolor [sit amet](https://sit.amet), consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum `dolor sit **amet,** consectetur` adipiscing elit.'))
                .toBe('Lorem ipsum <pre class="yamb-code yamb-code_inline"><code class="yamb-code__text text">dolor sit **amet,** consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum [dolor **sit**](https://dolor.site) amet, consectetur adipiscing elit.'))
                .toBe('Lorem ipsum <a href="https://dolor.site" target="_blank" rel="noopener noreferrer" class="link link_md">dolor **sit**</a> amet, consectetur adipiscing elit.');

            expect(formatText('Lorem ipsum [dolor sit amet](https://dolor.site/__amet__), consectetur adipiscing elit.'))
                .toBe('Lorem ipsum <a href="https://dolor.site/__amet__" target="_blank" rel="noopener noreferrer" class="link link_md">dolor sit amet</a>, consectetur adipiscing elit.');

            expect(formatText('Lorem ipsum [dolor ```sit```](https://dolor.site) amet, consectetur adipiscing elit.'))
                .toBe('Lorem ipsum <a href="https://dolor.site" target="_blank" rel="noopener noreferrer" class="link link_md">dolor ```sit```</a> amet, consectetur adipiscing elit.');
        });

        it('replaces nested stack tags', () => {
            expect(formatText('Lorem ipsum **dolor __sit amet,__ consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor <i class="text">sit amet,</i> consectetur</b> adipiscing elit.');

            expect(formatText('Lorem ipsum **dolor __sit amet, consectetur__** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor <i class="text">sit amet, consectetur</i></b> adipiscing elit.');

            expect(formatText('Lorem ipsum **__dolor sit amet,__ consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text"><i class="text">dolor sit amet,</i> consectetur</b> adipiscing elit.');

            expect(formatText('Lorem ipsum **__dolor sit amet, consectetur__** adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text"><i class="text">dolor sit amet, consectetur</i></b> adipiscing elit.');
        });

        it('ignores incorrect nested stack tags', () => {
            expect(formatText('Lorem ipsum **dolor __sit amet, consectetur** adipiscing__ elit.'))
                .toBe('Lorem ipsum <b class="text">dolor __sit amet, consectetur</b> adipiscing__ elit.');

            expect(formatText('Lorem ipsum **dolor __sit amet, consectetur**__ adipiscing elit.'))
                .toBe('Lorem ipsum <b class="text">dolor __sit amet, consectetur</b>__ adipiscing elit.');
        });

        it('replaces first tag when same nested tags', () => {
            expect(formatText('Lorem ipsum **dolor **sit amet,** consectetur** adipiscing elit.'))
                .toBe('Lorem ipsum <b class=\"text\">dolor **sit amet,</b> consectetur** adipiscing elit.');
        });

        it('replaces correct stack tag after incorrect', () => {
            expect(formatText('Lorem ipsum **dolor __sit amet, consectetur** __adipiscing__ elit.'))
                .toBe('Lorem ipsum <b class="text">dolor __sit amet, consectetur</b> <i class="text">adipiscing</i> elit.');
        });

        it('replaces stack tags whole text', () => {
            expect(formatText('**Lorem ipsum dolor sit amet, consectetur** adipiscing elit.'))
                .toBe('<b class="text">Lorem ipsum dolor sit amet, consectetur</b> adipiscing elit.');

            expect(formatText('Lorem ipsum **dolor sit amet, consectetur adipiscing elit.**'))
                .toBe('Lorem ipsum <b class="text">dolor sit amet, consectetur adipiscing elit.</b>');

            expect(formatText('**Lorem ipsum dolor sit amet, consectetur adipiscing elit.**'))
                .toBe('<b class="text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</b>');
        });

        it('replaces regular tags whole text', () => {
            expect(formatText('```Lorem ipsum dolor sit amet, consectetur``` adipiscing elit.'))
                .toBe('<pre class="yamb-code"><code class="yamb-code__text text">Lorem ipsum dolor sit amet, consectetur</code></pre> adipiscing elit.');

            expect(formatText('Lorem ipsum ```dolor sit amet, consectetur adipiscing elit.```'))
                .toBe('Lorem ipsum <pre class="yamb-code"><code class="yamb-code__text text">dolor sit amet, consectetur adipiscing elit.</code></pre>');

            expect(formatText('Lorem ipsum `dolor sit amet, consectetur adipiscing elit.`'))
                .toBe('Lorem ipsum <pre class="yamb-code yamb-code_inline"><code class="yamb-code__text text">dolor sit amet, consectetur adipiscing elit.</code></pre>');

            expect(formatText('```Lorem ipsum dolor sit amet, consectetur adipiscing elit.```'))
                .toBe('<pre class="yamb-code"><code class="yamb-code__text text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</code></pre>');
        });

        it('ignores doubled text in markdown link', () => {
            expect(formatText('Lorem ipsum [dolor sit][dolor sit](https://dolor.site) amet, consectetur adipiscing elit.')).toEqual(
                'Lorem ipsum [dolor sit]<a href="https://dolor.site" target="_blank" rel="noopener noreferrer" class="link link_md">dolor sit</a> amet, consectetur adipiscing elit.',
            );
        });

        it('replaces parens in link', () => {
            expect(formatText('Lorem ipsum https://dolor.site/?foo=bar() amet, consectetur adipiscing elit.'))
                .toBe('Lorem ipsum <a href="https://dolor.site/?foo=bar()" target="_blank" rel="noopener noreferrer" class="link">https://dolor.site/?foo=bar()</a> amet, consectetur adipiscing elit.');

            expect(formatText('Lorem ipsum [dolor sit](https://dolor.site/?foo=bar()) amet, consectetur adipiscing elit.'))
                .toBe('Lorem ipsum <a href="https://dolor.site/?foo=bar()" target="_blank" rel="noopener noreferrer" class="link link_md">dolor sit</a> amet, consectetur adipiscing elit.');

            expect(formatText('Lorem ipsum [dolor sit](https://dolor.site/?foo=bar()&filter=empty()) amet, consectetur adipiscing elit.'))
                .toBe('Lorem ipsum <a href="https://dolor.site/?foo=bar()&filter=empty()" target="_blank" rel="noopener noreferrer" class="link link_md">dolor sit</a> amet, consectetur adipiscing elit.');
        });

        it('formats cyrillic link', () => {
            expect(formatText(' президент.рф'))
                .toBe(' <a href="http://президент.рф" target="_blank" rel="noopener noreferrer" class="link">президент.рф</a>');

            expect(formatText('\nпрезидент.рф'))
                .toBe('<br /><a href="http://президент.рф" target="_blank" rel="noopener noreferrer" class="link">президент.рф</a>');
        });

        it('formats escape mdLink', () =>{
            expect(formatText('[test"></a><img src=x onerror=alert(1) />](https://ya.ru)'))
                .toBe('<a href="https://ya.ru" target="_blank" rel="noopener noreferrer" class="link link_md">test&quot;&gt;&lt;/a&gt;&lt;img src=x onerror=alert(1) /&gt;</a>');
        });
    });
});
