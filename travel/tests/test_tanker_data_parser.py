from unittest.case import TestCase

from travel.hotels.tools.region_pages_builder.renderer.renderer import tanker_data_parser as parser


class TestTankerDataParser(TestCase):
    def test_parse_question(self):
        question_html =\
            """
            <div class="question">
              <h3>Какие лучшие отели?</h3>
              <p>Все хорошие, особенно <a href="hotel:slug:good-city/good-hotel">вот этот</a></p>
            </div>
            """

        question_block = parser.HtmlParserWrapper().parse(question_html, parser.Question)

        self.assertEqual(
            parser.Question(
                question='Какие лучшие отели?',
                answer=parser.Paragraph(children=[
                    parser.PlainTextBlock(text='Все хорошие, особенно '),
                    parser.HotelLinkBlock(text='вот этот', slug="good-city/good-hotel"),
                ]),
                answer_raw_text='Все хорошие, особенно вот этот',
            ),
            question_block,
        )

    def test_raise_on_invalid_question_html(self):
        question_html =\
            """
            <div class="question">
              <p>Все хорошие, особенно <a href="hotel:slug:good-city/good-hotel">вот этот</a></p>
              <h3>Какие лучшие отели?</h3>
            </div>
            """

        with self.assertRaisesRegex(
            parser.HtmlDataParsingException,
            'Unexpected tag "p". Expected "h3".',
        ):
            parser.HtmlParserWrapper().parse(question_html, parser.Question)

    def test_raise_on_empty_div(self):
        question_html = \
            """
            <div class="question">
            </div>
            """

        with self.assertRaisesRegex(
            parser.HtmlDataParsingException,
            'Unexpected end of block. Expected "h3".',
        ):
            parser.HtmlParserWrapper().parse(question_html, parser.Question)

    def test_parse_faq(self):
        faq_html =\
            """
            <div>
              <h2>Частые вопросы об отелях в Гондоре</h2>
              <div class="question">
                <h3>Какие лучшие отели?</h3>
                <p>Все хорошие, особенно <a href="hotel:slug:good-city/good-hotel">вот этот</a></p>
              </div>
              <div class="question">
                <h3>Какие худшие отели?</h3>
                <p>Таких нет</p>
              </div>
            </div>
            """

        faq_block = parser.HtmlParserWrapper().parse(faq_html, parser.FaqRenderedBlock)

        self.assertEqual(
            parser.FaqRenderedBlock(
                title='Частые вопросы об отелях в Гондоре',
                questions=[
                    parser.Question(
                        question='Какие лучшие отели?',
                        answer=parser.Paragraph(children=[
                            parser.PlainTextBlock(text='Все хорошие, особенно '),
                            parser.HotelLinkBlock(text='вот этот', slug="good-city/good-hotel"),
                        ]),
                        answer_raw_text='Все хорошие, особенно вот этот',
                    ),
                    parser.Question(
                        question='Какие худшие отели?',
                        answer=parser.Paragraph(
                            children=[parser.PlainTextBlock(text='Таких нет')],
                        ),
                        answer_raw_text='Таких нет',
                    )
                ],
            ),
            faq_block,
        )

    def test_parse_spoiler(self):
        spoiler_html =\
            """
            <div class="spoiler">
              <h3>Заголовок спойлера</h3>
              <p>Контент спойлера</p>
            </div>
            """

        spoiler_block = parser.HtmlParserWrapper().parse(spoiler_html, parser.SpoilerTextBlock)

        self.assertEqual(
            parser.SpoilerTextBlock(
                title="Заголовок спойлера",
                description=parser.Paragraph(children=[
                    parser.PlainTextBlock(text="Контент спойлера"),
                ]),
            ),
            spoiler_block,
        )

    def test_raises_on_doubled_spoiler_content(self):
        spoiler_html =\
            """
            <div class="spoiler">
              <h3>Заголовок спойлера</h3>
              <p>Контент спойлера</p>
              <p>Лишний контент, которого не должно быть</p>
            </div>
            """

        with self.assertRaisesRegex(
            parser.HtmlDataParsingException,
            'Unexpected tag "p". Expected end of block.',
        ):
            parser.HtmlParserWrapper().parse(spoiler_html, parser.SpoilerTextBlock)

    def test_parse_subsection(self):
        subsection_html =\
            """
            <div class="subsection">
              <h3>Заголовок субсекции</h3>
              <p>Контент субсекции</p>
              <p>Контент субсекции дубль 2</p>
            </div>
            """

        spoiler_block = parser.HtmlParserWrapper().parse(
            subsection_html,
            parser.SubSectionTextBlock,
        )

        self.assertEqual(
            parser.SubSectionTextBlock(
                title="Заголовок субсекции",
                paragraphs=[
                    parser.Paragraph(children=[
                        parser.PlainTextBlock(text="Контент субсекции"),
                    ]),
                    parser.Paragraph(children=[
                        parser.PlainTextBlock(text="Контент субсекции дубль 2"),
                    ]),
                ],
            ),
            spoiler_block,
        )

    def test_parse_text_block(self):
        html =\
            """
            <div class="section">
              <h2>Лучшие отели в городе Гондор</h2>
              <p>Какой-то хитрый <i>курсив</i> про то какой отель лучший, а какой отель не очень.</p>
              <div class="spoiler">
                <h3>Заголовок спойлера</h3>
                <p>Контент спойлера</p>
              </div>
              <div class="subsection">
                <h3>Заголовок субсекции</h3>
                <p>Контент субсекции</p>
              </div>
            </div>
            """

        text_block = parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)

        self.assertEqual(
            parser.TextRenderedBlock(
                title="Лучшие отели в городе Гондор",
                children=[
                    parser.Paragraph(children=[
                        parser.PlainTextBlock(text="Какой-то хитрый "),
                        parser.PlainTextBlock(
                            text="курсив",
                            styles=[parser.PlainTextBlockStyle.ITALIC],
                        ),
                        parser.PlainTextBlock(text=" про то какой отель лучший, а какой отель не очень."),
                    ]),
                    parser.SpoilerTextBlock(
                        title="Заголовок спойлера",
                        description=parser.Paragraph(children=[
                            parser.PlainTextBlock(text="Контент спойлера")
                        ]),
                    ),
                    parser.SubSectionTextBlock(
                        title="Заголовок субсекции",
                        paragraphs=[
                            parser.Paragraph(children=[
                                parser.PlainTextBlock(text="Контент субсекции")
                            ])
                        ]
                    )
                ]
            ),
            text_block,
        )

    def test_parse_price(self):
        html =\
            """
            <div class="section">
              <h2>Цена вопроса?</h2>
              <p><span class=\"price\" currency=\"RUB\">100</span> рублей</p>
            </div>
            """

        block = parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)

        self.assertEqual(
            block,
            parser.TextRenderedBlock(
                title="Цена вопроса?",
                children=[
                    parser.Paragraph(children=[
                        parser.PriceTextBlock(
                            parser.Price("100", parser.Currency.RUB)
                        ),
                        parser.PlainTextBlock(" рублей"),
                    ])
                ]
            )
        )

    def test_parse_question_with_price(self):
        html =\
            """
            <div>
              <h2>Вопросы</h2>
              <div class="question">
                <h3>Цена вопроса?</h3>
                <p>Ни много ни мало - <span class=\"price\" currency=\"RUB\">100</span></p>
              </div>
            </div>
            """

        block = parser.HtmlParserWrapper().parse(html, parser.FaqRenderedBlock)

        self.assertEqual(
            block,
            parser.FaqRenderedBlock(
                title="Вопросы",
                questions=[
                    parser.Question(
                        question="Цена вопроса?",
                        answer=parser.Paragraph(children=[
                            parser.PlainTextBlock("Ни много ни мало - "),
                            parser.PriceTextBlock(
                                parser.Price("100", parser.Currency.RUB)
                            ),
                        ]),
                        answer_raw_text="Ни много ни мало - 100 руб.",
                    )
                ]
            )
        )

    def test_price_validated(self):
        parser.Price("123", parser.Currency.RUB)

        with self.assertRaises(ValueError):
            parser.Price("Not a price", parser.Currency.RUB)

    def test_parse_text_with_style(self):
        html =\
            """
            <div class="section">
              <p>Текст с <i>курсивом</i>, <b>жирным</b> и <i><b>двойным</b></i> написанием.</p>
            </div>
            """

        block = parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)

        self.assertEqual(
            parser.TextRenderedBlock(children=[
                parser.Paragraph(children=[
                    parser.PlainTextBlock("Текст с "),
                    parser.PlainTextBlock(
                        "курсивом",
                        styles=[parser.PlainTextBlockStyle.ITALIC],
                    ),
                    parser.PlainTextBlock(", "),
                    parser.PlainTextBlock("жирным", styles=[parser.PlainTextBlockStyle.BOLD]),
                    parser.PlainTextBlock(" и "),
                    # TODO: Тут не полное соответствие спецификации, двойной стиль не поддерживается
                    parser.PlainTextBlock("двойным", styles=[parser.PlainTextBlockStyle.ITALIC]),
                    parser.PlainTextBlock(" написанием."),
                ])
            ]),
            block,
        )

    def test_raise_on_multiple_divs(self):
        html =\
            """
            <div class="section">
              <p>Это нормально</p>
            </div>
            <div class="section">
              <p>А это нет</p>
            </div>
            """

        with self.assertRaises(Exception):
            parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)

    def test_raise_on_mailformed_link(self):
        html =\
            """
            <div class="section">
              <p><a>Ссылка без атрибута href</a></p>
            </div>
            """

        with self.assertRaisesRegex(parser.HtmlDataParsingException, "Mailformed link"):
            parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)

    def test_raise_on_incorrect_class(self):
        html =\
            """
            <div class="not-a-section">
            </div>
            """

        with self.assertRaisesRegex(
            parser.HtmlDataParsingException,
            'Unknown tag "div.not-a-section". Expected "div.section".',
        ):
            parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)

    def test_raise_on_unknown_tag(self):
        html =\
            """
            <div class="section">
              <p>Текст</p>
              <code>a = 1</code>
            </div>
            """

        with self.assertRaisesRegex(
            parser.HtmlDataParsingException,
            'Unknown tag "code". Expected one of "p", "div.spoiler", "div.subsection", end of block.',
        ):
            parser.HtmlParserWrapper().parse(html, parser.TextRenderedBlock)
