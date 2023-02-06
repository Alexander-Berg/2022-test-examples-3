import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CurrentUserReview} currentUserReview
 * @param {PageObject.Review} review
 * @param {PageObject.components.Review.Header} header
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок «Ваш отзыв»', {
    feature: 'Рейтинг магазина',
    story: {
        'По умолчанию': {
            'отображается заголовок, кнопки управления, имя, аватар, дата, оценка, текст отзыва, лайк/дизлайк':
                makeCase({
                    id: 'm-touch-2470',
                    issue: 'MOBMARKET-10333',
                    params: {
                        pro: 'Достоинства',
                        contra: 'Недостатки',
                        comment: 'Комментарий',
                    },
                    async test() {
                        await this.expect(this.currentUserReview.isVisible())
                            .to.be.equal(true, 'Блок «Ваш отзыв» отображается');
                        await this.expect(this.currentUserReview.getTitleText())
                            .to.be.equal('Ваш отзыв', 'Заголовок «Ваш отзыв» отображается');

                        await this.header.openReviewMenu();

                        await this.expect(this.header.isEditButtonVisible())
                            .to.be.equal(true, 'Кнопка «Изменить» отображается');
                        await this.expect(this.header.isRemoveButtonVisible())
                            .to.be.equal(true, 'Кнопка «Удалить» отображается');

                        await this.header.closeReviewMenu();

                        await this.expect(this.header.getAuthorNameText())
                            .to.be.equal('somePublicDisplayName', 'Имя автора отображается');
                        await this.expect(this.header.getAvatarUrl()).to.be.equal(
                            'https://avatars.mds.yandex.net/get-yapic/36777/515095637-1546054309/islands-retina-50',
                            'Аватар отображается'
                        );
                        await this.expect(this.header.getDateText())
                            .to.be.equal('1 января 2015', 'Дата отображается');
                        await this.expect(this.review.getGradeText())
                            .to.be.equal('Плохой магазин', 'Поставленная оценка отображается');

                        await this.review.clickExpander();

                        await this.expect(this.review.getProText())
                            .to.be.equal(this.params.pro, 'Текст достоинств отображается');
                        await this.expect(this.review.getContraText())
                            .to.be.equal(this.params.contra, 'Текст недостатков отображается');
                        await this.expect(this.review.getCommentText())
                            .to.be.equal(this.params.comment, 'Текст комментария отображается');

                        await this.expect(this.votes.getLikeCount())
                            .to.be.equal(300, 'Лайки отображаются');

                        await this.expect(this.votes.getDislikeCount())
                            .to.be.equal(200, 'Дизлайки отображаются');
                    },
                }),
        },
    },
});
