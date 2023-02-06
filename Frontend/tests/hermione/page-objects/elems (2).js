module.exports = El => {
    const PO = {};

    PO.Ad = new El({ block: 'Ad' });

    PO.link = new El({ block: 'Link' });
    PO.button = new El({ block: 'Button2' });
    PO.checkedButton = PO.button.mods({ checked: true });
    PO.disabledButton = PO.button.mods({ disabled: true });
    PO.popup = new El({ block: 'popup2' });
    PO.menu = new El({ block: 'menu' });
    PO.menuItem = new El({ block: 'menu__item' });

    PO.visiblePopup = new El({ block: 'popup2', modName: 'visible', modVal: 'yes' });
    PO.visiblePopup.menu = PO.menu.copy();
    PO.visiblePopup.lastMenuItem = PO.menuItem.copy().lastOfType();

    PO.checkbox = new El({ block: 'Сheckbox' });
    PO.checkbox.label = new El({ block: 'Сheckbox-Label' });

    PO.Row = new El({ block: 'Row' });

    PO.AnimateHeightAuto = new El({ block: 'rah-static--height-auto' });
    PO.AnimateHeightPartial = new El({ block: 'rah-static--height-specific' });
    PO.AnimateHeightHidden = new El({ block: 'rah-static--height-zero' });

    // desktop
    PO.HeadTabs = new El({ block: 'yandex-header__nav' });
    PO.HeadTabs.LinkItem = new El({ block: 'yandex-header__nav-link' });
    PO.HeadTabs.LinkItemActive = PO.HeadTabs.LinkItem.copy().mods({ active: 'yes' });
    PO.HeadTabs.LinkItemFirst = PO.HeadTabs.LinkItem.copy().nthChild(1);
    PO.HeadTabs.LinkItemSecond = PO.HeadTabs.LinkItem.copy().nthChild(2);
    PO.HeadTabs.LinkItemThird = PO.HeadTabs.LinkItem.copy().nthChild(3);
    PO.HeadTabs.LinkItemFourth = PO.HeadTabs.LinkItem.copy().nthChild(4);
    PO.HeadTabs.LinkItemFifth = PO.HeadTabs.LinkItem.copy().nthChild(5);
    PO.HeadTabs.LinkItemSixth = PO.HeadTabs.LinkItem.copy().nthChild(6);
    PO.HeadTabs.LinkItemSeventh = PO.HeadTabs.LinkItem.copy().nthChild(7);

    // touch
    PO.HeadTabsTouch = new El({ block: 'HeadTabs' });
    PO.ScrollerItem = new El({ block: 'Scroller', elem: 'Item' });
    PO.LinkItem = new El({ block: 'HeadTabs', elem: 'LinkItem' });
    PO.HeadTabsTouch.LinkItemActive = PO.ScrollerItem.copy().descendant(PO.LinkItem.copy().mods({ active: true }));
    PO.HeadTabsTouch.LinkItemFirst = PO.ScrollerItem.copy().nthChild(1).descendant(PO.LinkItem.copy());
    PO.HeadTabsTouch.LinkItemSecond = PO.ScrollerItem.copy().nthChild(2).descendant(PO.LinkItem.copy());
    PO.HeadTabsTouch.LinkItemThird = PO.ScrollerItem.copy().nthChild(3).descendant(PO.LinkItem.copy());
    PO.HeadTabsTouch.LinkItemFourth = PO.ScrollerItem.copy().nthChild(4).descendant(PO.LinkItem.copy());
    PO.HeadTabsTouch.LinkItemFifth = PO.ScrollerItem.copy().nthChild(5).descendant(PO.LinkItem.copy());
    PO.HeadTabsTouch.LinkItemSixth = PO.ScrollerItem.copy().nthChild(6).descendant(PO.LinkItem.copy());
    PO.HeadTabsTouch.LinkItemSeventh = PO.ScrollerItem.copy().nthChild(7).descendant(PO.LinkItem.copy());

    // desktop
    PO.Header = new El({ block: 'yandex-header' });
    PO.Header.SearchInput = new El({ block: 'yandex-header__input' });
    PO.Header.SearchSubmit = new El({ block: 'yandex-header__submit' });
    PO.Header.UserStatistics = new El({ block: 'yandex-header__action-button' });
    PO.Header.Logo = new El({ block: 'yandex-header__logo' });
    PO.Header.LogoLinkYandex = new El({ block: 'yandex-header__logo-base' });
    PO.Header.LogoLinkTutor = new El({ block: 'yandex-header__logo-service' });

    // touch
    PO.HeaderTouch = new El({ block: 'turbo-header' });
    PO.HeaderTouch.SearchIcon = new El({ block: 'turbo-header__search-icon' });
    PO.HeaderTouch.SearchInput = new El({ block: 'turbo-header__control' });
    PO.HeaderTouch.Logo = new El({ block: 'turbo-header__logo' });
    PO.HeaderTouch.LogoLinkYandex = new El({ block: 'turbo-header__logo-base' });
    PO.HeaderTouch.LogoLinkTutor = new El({ block: 'turbo-header__logo-service' });

    PO.User = new El({ block: 'legouser' });
    PO.UserPopup = new El({ block: 'legouser__popup' });
    PO.UserEnter = new El({ block: 'login-button' });
    PO.UserRegistration = new El({ block: 'signup-link' });

    PO.SubjectItem = new El({ block: 'SubjectItem' });
    PO.SubjectItemFirst = PO.SubjectItem.nthChild(1).descendant(new El('a'));
    PO.SubjectItemSecond = PO.SubjectItem.nthChild(2).descendant(new El('a'));
    PO.SubjectItemThird = PO.SubjectItem.nthChild(3).descendant(new El('a'));
    PO.SubjectItemFourth = PO.SubjectItem.nthChild(4).descendant(new El('a'));

    PO.Catalog = new El({ block: 'Catalog' });
    PO.Catalog.SubsSection = new El({ block: 'Catalog', elem: 'SubSection' });
    PO.Catalog.TopicNameFirst = new El({ block: 'Catalog', elem: 'TopicName' }).nthChild(2);
    PO.Catalog.SubsFirst = new El({ block: 'Catalog', elem: 'Subs' }).nthChild(3);
    PO.Catalog.SubsFirstOpened = new El({ block: 'Catalog', elem: 'Subs' }).nthChild(3).mix(PO.AnimateHeightAuto.copy());
    PO.Catalog.SubsFirst.SubSectionFirst = new El({ block: 'Catalog', elem: 'SubSection' }).firstOfType();
    PO.PromoSubscribe = new El({ block: 'PromoSubscribe' });
    PO.PromoSubscribe.Btn = new El({ block: 'PromoSubscribe', elem: 'Btn' });

    PO.TopicListCard = new El({ block: 'TopicListCard' });
    PO.TopicListCard.Catalog = PO.Catalog.copy();
    PO.TopicListCard.Catalog.RowFirst = new El({ block: 'Catalog', elem: 'Row' }).nthChild(2);
    PO.TopicListCard.Catalog.RowFirst.TopicNameFirst = new El({ block: 'Catalog', elem: 'TopicName' });
    PO.TopicListCard.Catalog.SubsFirst = PO.Catalog.SubsFirst.copy();
    PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst = PO.Catalog.SubsFirst.SubSectionFirst.copy();
    PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.Text = new El({ block: 'Text' });
    PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.StatusCount = new El({ block: 'Stats', elem: 'Count' });
    PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.Link = new El({ block: 'TopicListCard', elem: 'LinkText' })
        .descendant(new El('a'));
    PO.TopicListCard.CatalogSubs = PO.Catalog.SubsFirst.copy();

    PO.LessonsListCard = new El({ block: 'LessonsListCard' });
    PO.LessonsListCard.Catalog = PO.Catalog.copy();
    PO.LessonsListCard.Catalog.RowFirst = new El({ block: 'Catalog', elem: 'Row' }).nthChild(2);
    PO.LessonsListCard.Catalog.RowFirst.TopicNameFirst = new El({ block: 'Catalog', elem: 'TopicName' });
    PO.LessonsListCard.Catalog.SubsFirst = PO.Catalog.SubsFirst.copy();
    PO.LessonsListCard.Catalog.SubsFirst.SubSectionFirst = PO.Catalog.SubsFirst.SubSectionFirst.copy();
    PO.LessonsListCard.Catalog.SubsFirst.SubSectionFirst.Text = new El({ block: 'Text' });
    PO.LessonsListCard.Catalog.SubsFirst.SubSectionFirst.StatusCount = new El({ block: 'Stats', elem: 'Count' });
    PO.LessonsListCard.Catalog.SubsFirst.SubSectionFirst.Link = new El({ block: 'LessonsListCard', elem: 'LinkText' })
        .descendant(new El('a'));
    PO.LessonsListCard.CatalogSubs = PO.Catalog.SubsFirst.copy();

    PO.MiniSearch = new El({ block: 'MiniSearch' });
    PO.MiniSearchControls = new El({ block: 'MiniSearch', elem: 'Controls' });
    PO.MiniSearchControls.input = new El('input');
    PO.MiniSearchControls.button = new El('button');
    PO.MiniSearchError = new El({ block: 'MiniSearch', elem: 'Error' });

    PO.PromoControls = new El({ block: 'Promo', elem: 'Controls' });
    PO.PromoControls.Link = new El('a');
    PO.PromoDescription = new El({ block: 'Promo', elem: 'Description' });
    PO.PromoDescription.Link = new El('a');

    PO.PromoReference = new El({ block: 'PromoReference' });
    PO.PromoReference.Btn = new El({ block: 'PromoReference', elem: 'Btn' });

    PO.Footer = new El({ block: 'Footer' });
    PO.Footer.RulesLink = new El({ block: 'Footer', elem: 'Rules' });
    PO.Footer.SupportLink = new El({ block: 'Footer', elem: 'Support' });
    PO.Footer.AboutLink = new El({ block: 'Footer', elem: 'About' });
    PO.Footer.FeedbackLink = new El({ block: 'Footer', elem: 'Feedback' });
    PO.Footer.StatisticsButton = new El({ block: 'Footer', elem: 'Button', modName: 'statistics', modVal: true });
    PO.FooterCopyright = PO.Footer.descendant(new El({ block: 'copyright__dates' }));

    PO.Identifier = new El({ block: 'Identifier' });

    PO.EduCard = new El({ block: 'EduCard' });
    PO.EduCardSwitcher = new El({ block: 'EduCard', elem: 'Switcher' });
    PO.EduCardVariant = new El({ block: 'EduCard', elem: 'Variants' });
    PO.EduCardTitle = new El({ block: 'EduCard', elem: 'Title' });
    PO.EduCardVariant.VariantButtonFirst = new El({ block: 'VariantButton_extended' });
    PO.EduCardVariant.Container = new El({ block: 'EduCard', elem: 'Container' });
    PO.EduCardVariant.Container.ColLasts = new El({ block: 'Col' }).lastOfType();
    PO.EduCardVariant.Container.ColLasts.Button = new El({ block: 'Text' });

    PO.LessonPage = new El({ block: 'LessonPage' });
    PO.LessonPage.RightBlock = new El({ block: 'LessonPage', elem: 'RightBlock' });

    PO.LessonTabs = new El({ block: 'LessonTabs' });
    PO.LessonTabs.Main = new El({ block: 'LessonTabs', elem: 'Main' });
    PO.LessonTabs.Select = new El({ block: 'LessonTabs', elem: 'Select' });
    PO.LessonTabs.Select.active = PO.checkedButton.copy();
    PO.LessonTabs.second = PO.LessonTabs.Select.nthChild(2);
    PO.LessonTabs.third = PO.LessonTabs.Select.nthChild(3);
    PO.LessonTabs.forth = PO.LessonTabs.Select.nthChild(4);

    PO.LessonSelect = new El({ block: 'LessonSelect' });
    PO.LessonSelect.Button = new El({ block: 'LessonSelect', elem: 'Button' });

    PO.LessonThemeSwitcher = new El({ block: 'LessonThemeSwitcher' });
    PO.LessonThemeSwitcher.button = PO.button.copy();

    PO.Scoremeter = new El({ block: 'Scoremeter' });
    PO.Scoremeter.Head = new El({ block: 'Scoremeter', elem: 'Head' });
    PO.Scoremeter.HeadCounter = new El({ block: 'Scoremeter', elem: 'HeadCounter' });
    PO.Scoremeter.SubjectCount = new El({ block: 'Scoremeter', elem: 'SubjectCount' });
    PO.Scoremeter.Subject = new El({ block: 'Scoremeter', elem: 'Subject' }).firstOfType();
    PO.Scoremeter.Subject.Number = new El({ block: 'Scoremeter', elem: 'SubjectCount' });

    PO.TaskCriteria = new El({ block: 'TaskCriteria' });

    PO.Task = new El({ block: 'Task' });
    PO.Task.input = new El({ block: 'Textinput-Control' });
    PO.TaskTitle = new El({ block: 'Task', elem: 'Title' });
    PO.TaskTitle.Id = PO.Identifier.copy();
    PO.Task.Title = PO.TaskTitle.copy();
    PO.Task.ReportLink = new El({ block: 'Task', elem: 'ReportLink' });
    PO.Task.InfoIcon = new El({ block: 'Task', elem: 'InfoIcon' });
    PO.Task.AddButton = new El({ block: 'Task', elem: 'AddButton' });
    PO.Task.AnswerButtons = new El({ block: 'AnswerButtons' });
    PO.Task.AnswerButtons.button = new El({ block: 'AnswerButtons', elem: 'Button' });
    PO.Task.AnswerButtons.firstButton = PO.Task.AnswerButtons.button.copy().nthChild(1);
    PO.Task.AnswerButtons.secondButton = PO.Task.AnswerButtons.button.copy().nthChild(2);
    PO.Task.AnswerButtons.thirdButton = PO.Task.AnswerButtons.button.copy().nthChild(3);

    PO.AddToVariantModal = new El({ block: 'AddToVariantModal' });
    PO.AddToVariantModal.Close = new El({ block: 'AddToVariantModal', elem: 'Close' });
    PO.AddToVariantModal.Item = new El({ block: 'AddToVariantModal', elem: 'Item' });
    PO.AddToVariantModal.Spinner = new El({ block: 'AddToVariantModal', elem: 'Spinner' });
    PO.AddToVariantModal.AddFariant = PO.AddToVariantModal.Item.copy().firstOfType();
    PO.AddToVariantModal.FirstVariant = PO.AddToVariantModal.Item.copy().nthChild(2);
    PO.AddToVariantModal.SecondVariant = PO.AddToVariantModal.Item.copy().nthChild(3);
    PO.AddToVariantModal.LastVariant = PO.AddToVariantModal.Item.copy().lastOfType();

    PO.Alert = new El({ block: 'Alert' });

    PO.CreateVariantForm = new El({ block: 'CreateVariantForm' });
    PO.CreateVariantForm.Close = new El({ block: 'CreateVariantForm', elem: 'Close' });
    PO.CreateVariantForm.Cancel = new El({ block: 'CreateVariantForm', elem: 'Cancel' });
    PO.CreateVariantForm.Submit = new El({ block: 'CreateVariantForm', elem: 'Submit' });
    PO.CreateVariantForm.TitleInput = new El({ block: 'CreateVariantForm', elem: 'TitleInput' });
    PO.CreateVariantForm.TimeInput = new El({ block: 'CreateVariantForm', elem: 'TimeInput' });
    PO.CreateVariantForm.SubmitButtonDisabled =
        new El({ block: 'CreateVariantForm', elem: 'Submit', modName: 'disabled', modVal: true });
    PO.CreateVariantForm.TitleInput.input = new El('input');
    PO.CreateVariantForm.TimeInput.input = new El('input');

    PO.EditVariantButton = new El({ block: 'EditVariantButton' });

    PO.VariantSectionAB = new El({ block: 'VariantTestSectionAB' });
    PO.VariantSectionAB.Item = new El({ block: 'VariantTestSectionAB', elem: 'TaskItem' });
    PO.VariantSectionAB.Item.input = new El('input');
    PO.VariantSectionAB.ItemFirst = PO.VariantSectionAB.Item.copy().firstOfType();
    PO.VariantSectionAB.ItemNth = number => PO.VariantSectionAB.Item.copy().nthChild(number);
    PO.VariantWizard = new El({ block: 'VariantTestWizard' });

    PO.TaskBlockImage = new El({ block: 'TaskBlock', modName: 'type', modVal: 'image' });
    PO.TaskBlockVideo = new El({ block: 'TaskBlock', modName: 'type', modVal: 'video' });
    PO.TaskBlockModal = new El({ block: 'TaskBlock', elem: 'Modal' });
    PO.TaskBlockModalVisible = new El({ block: 'TaskBlock', elem: 'Modal', modName: 'visible' });
    PO.TaskBlockModal.Img = new El({ block: 'TaskBlock', elem: 'Img' });

    PO.TaskResultLine = new El({ block: 'Task', elem: 'ResultLine' });
    PO.TaskResultLine.ToggleAnswer = new El({ block: 'Text' });
    PO.TaskResultLine.VisibleAnswer = PO.AnimateHeightAuto.copy();
    PO.TaskResultLine.Answer = new El({ block: 'Row' }).lastOfType().descendant(new El({ block: 'Text' }).lastOfType());
    PO.TaskBlockQuote = new El({ block: 'TaskBlock', elem: 'Quote' });
    PO.TaskBlockQuote.Visible = PO.AnimateHeightAuto.copy();
    PO.TaskBlockQuote.Hidden = PO.AnimateHeightPartial.copy();
    PO.TaskQuoteLink = new El({ block: 'TaskBlock', elem: 'QuoteLink' });
    PO.TaskControlLine = new El({ block: 'Task', elem: 'ControlLine' });
    PO.TaskControlLine.button = new El({ block: 'Button2' });
    PO.TaskControlLine.extraButton = PO.TaskControlLine.button.copy().nthType(2);
    PO.TaskControlLine.input = new El({ block: 'Textinput-Control' });
    PO.TaskToggleAnswerText = new El({ block: 'Task', elem: 'ToggleAnswer' }).descendant(new El({ block: 'Text' }));
    PO.TaskMistakeInfoPopup = new El({ block: 'Task', elem: 'MistakeInfoPopup' });

    PO.LessonContentResolver = new El({ block: 'LessonContentResolver' });
    PO.LessonContentResolver.Loading = new El({ block: 'LessonContentResolver', elem: 'Loading' });
    PO.LessonContentResolver.Task = PO.Task.copy();
    PO.LessonContentResolver.FirstRow = new El({ block: 'LessonContentResolver', elem: 'TestRow' }).firstOfType();
    PO.LessonContentResolver.FirstRow.Task = PO.Task.copy();

    PO.TimerCard = new El({ block: 'TimerCard' });

    PO.TimerClock = new El({ block: 'TimerClock' });
    PO.TimerClock.Timer = new El({ block: 'TimerClock', elem: 'Time' });
    PO.TimerClock.Description = new El({ block: 'TimerClock', elem: 'Description' });

    PO.TimerClock.TestInfo = new El({ block: 'TimerClock', elem: 'TestInfo' });
    PO.TimerClock.TestInfo.FirstRow = new El({ block: 'Text' }).nthChild(1);
    PO.TimerClock.Reference = new El({ block: 'TimerClock', elem: 'Reference' });
    PO.TimerClock.Switcher = new El({ block: 'TimerClock', elem: 'Switcher' });
    PO.TimerClock.SolvedCounter = new El({ block: 'TimerClock', elem: 'SolvedCounter' });

    PO.Pause = new El({ block: 'Pause' });
    PO.Pause.TaskCount = new El({ block: 'Pause', elem: 'Info' }).nthChild(2);
    PO.Pause.Button = new El({ block: 'Pause', elem: 'Button' });

    PO.TheoryMiniViewer = new El({ block: 'TheoryMiniViewer' });

    PO.TextInputNone = new El({ block: 'TextInput', modName: 'answer', modVal: 'none' });
    PO.TextInputWrong = new El({ block: 'TextInput', modName: 'answer', modVal: 'wrong' });
    PO.TextInputCorrect = new El({ block: 'TextInput', modName: 'answer', modVal: 'correct' });
    PO.TextInputRed = new El({ block: 'TextInput', modName: 'color', modVal: 'red' });

    PO.UpperCard = new El({ block: 'UpperCard' });
    PO.UpperCardId = new El({ block: 'UpperCard', elem: 'Id' });
    PO.UpperCard.Variants = new El({ block: 'EduCard', elem: 'Variants' });

    PO.VariantButtonExtended = new El({ block: 'VariantButton', modName: 'extended', modVal: true });
    PO.VariantButtonExtended.Cover = new El({ block: 'VariantButton', elem: 'Cover', modName: 'desktop', modVal: true });

    PO.VariantTip = new El({ block: 'VariantTip' });
    PO.VariantTipPopup = new El({ block: 'VariantTip', elem: 'Popup' });
    PO.VariantTipModal = new El({ block: 'VariantTip', elem: 'Modal' });
    PO.VariantTipButton = new El({ block: 'VariantTip', elem: 'Button' });
    PO.VariantTipClose = new El({ block: 'VariantTip', elem: 'Close' });
    PO.VariantTipPrintLink = new El({ block: 'VariantTip', elem: 'PrintLink' });

    PO.RelatedLinks = new El({ block: 'RelatedLinks' });
    PO.RelatedLinks.FirstLink = new El({ block: 'RelatedLinks', elem: 'Link' })
        .firstOfType()
        .descendant(new El('a'));

    PO.RelatedLinksFragmentFirstLink = new El({ block: 'RelatedLinks', elem: 'Fragment' })
        .firstOfType()
        .descendant(new El({ block: 'RelatedLinks', elem: 'Link' }))
        .firstOfType()
        .descendant(new El('a'));
    PO.RelatedLinksFragmentLastLink = new El({ block: 'RelatedLinks', elem: 'Fragment' })
        .lastOfType()
        .descendant(new El({ block: 'RelatedLinks', elem: 'Link' }))
        .firstOfType()
        .descendant(new El('a'));

    PO.Modal = new El({ block: 'Modal' });
    PO.VisibleModal = new El({ block: 'Modal', modName: 'visible' });
    PO.ModalOpened = new El({ block: 'TaskBlock', elem: 'Modal' });

    PO.Variant = new El({ block: 'Variant' });
    PO.VariantLastTask = new El({ block: 'Variant', elem: 'TaskItem' }).lastOfType();
    PO.VariantPrintLink = new El({ block: 'UpperCard', elem: 'Links' }).descendant(PO.button);
    PO.VariantTestPageTimerCardContainer = new El({ block: 'VariantTestPage', elem: 'TimerCardContainer' });
    PO.VariantTestPageStickyContent = new El({ block: 'VariantTestPage', elem: 'StickyContent' });
    PO.FinishButton = new El({ block: 'VariantFinishButton-Control' });
    PO.LockTest = new El({ block: 'LockTest' });

    PO.SideBlock = new El({ block: 'SideBlock' });
    PO.OpenSideBlock = new El({ block: 'SideBlock' }).mods({ open: true });
    PO.SideBlock.Header = new El({ block: 'SideBlock', elem: 'HeaderRow' });
    PO.SideBlock.Header.TheoryLink = PO.link;
    PO.SideBlock.Header.closeButton = PO.button.copy();
    PO.SideBlock.TaskBlockImage = PO.TaskBlockImage.copy();

    PO.TasksToSolveList = new El({ block: 'TaskToSolve' });

    PO.TasksToSolveListLastItem = new El({ block: 'TaskToSolve' }).lastOfType();
    PO.TasksToSolveListLastItem.Task = PO.Task.copy();
    PO.TasksToSolveListLastItem.Input = PO.TaskControlLine.input.copy();
    PO.TasksToSolveListLastItem.InputWrong = PO.TextInputWrong.copy();
    PO.TasksToSolveListLastItem.InputCorrect = PO.TextInputCorrect.copy();
    PO.TasksToSolveListLastItem.TaskControlLine = PO.TaskControlLine.copy();
    PO.TasksToSolveListLastItem.TaskControlLine.Button = PO.TaskControlLine.button.copy();

    PO.TasksToSolveListFirstItem = new El({ block: 'TaskToSolve' }).firstOfType();
    PO.TasksToSolveListFirstItem.Task = PO.Task.copy();
    PO.TasksToSolveListFirstItem.Task.Title = new El({ block: 'Title' });
    PO.TasksToSolveListFirstItem.Task.Title.Id = new El({ block: 'Identifier' }).firstOfType();
    PO.TasksToSolveListFirstItem.Task.ReportLink = new El({ block: 'Task', elem: 'ReportLink' });
    PO.TasksToSolveListFirstItem.Input = PO.TaskControlLine.input.copy();
    PO.TasksToSolveListFirstItem.InputWrong = PO.TextInputWrong.copy();
    PO.TasksToSolveListFirstItem.TaskControlLine = PO.TaskControlLine.copy();
    PO.TasksToSolveListFirstItem.TaskControlLine.Button = PO.TaskControlLine.button.copy();

    PO.PageLayout = new El({ block: 'PageLayout' });
    PO.PageLayout.Container = new El({ block: 'PageLayout', elem: 'Container' });
    PO.PageLayout.Content = new El({ block: 'PageLayout', elem: 'Content' });
    PO.PageLayout.Left = new El({ block: 'PageLayout', elem: 'Left' });
    PO.PageLayout.Right = new El({ block: 'PageLayout', elem: 'Right' });

    PO.Report = new El({ block: 'Report' });
    PO.Report.Title = new El({ block: 'Report', elem: 'Title' });
    PO.Report.Time = new El({ block: 'Report', elem: 'Time' });
    PO.Report.Statistic = new El({ block: 'Report', elem: 'Statistic' });
    PO.Report.PointsInfo = new El({ block: 'Report', elem: 'PointsInfo' });

    PO.UserStatistics = new El({ block: 'UserStatistics' });
    PO.UserStatistics.More = new El({ block: 'UserStatistics', elem: 'More' });
    PO.UserStatistics.More.Button = PO.button;
    PO.UserStatistics.Tab = new El({ block: 'UserStatistics', elem: 'Tab' });
    PO.UserStatistics.Tab.activeButton = new El({ block: '.Button2[aria-pressed=true]' });
    PO.UserStatistics.TabFirstButton = PO.UserStatistics.Tab.nthChild(1).descendant(PO.button);
    PO.UserStatistics.TabLastButton = PO.UserStatistics.Tab.nthChild(2).descendant(PO.button);
    PO.UserStatistics.Filters = new El({ block: 'UserStatistics', elem: 'Filters' });
    PO.UserStatistics.Filters.control = new El({ block: 'control' });
    PO.FilterFirst = PO.menu.copy().descendant(PO.menuItem.copy()).nthChild(1);
    PO.FilterSecond = PO.menu.copy().descendant(PO.menuItem.copy()).nthChild(2);
    PO.FilterThird = PO.menu.copy().descendant(PO.menuItem.copy()).nthChild(3);
    PO.UserStatistics.EmptyScreen = new El({ block: 'UserStatistics', elem: 'EmptyScreen' });
    PO.UserStatistics.EmptyScreen.Text = new El({ block: 'Text' });
    PO.UserStatisticsPendingScreenSuccess = new El({ block: 'UserStatistics', elem: 'PendingScreen', modName: 'mode', modVal: 'success' });
    PO.UserStatistics.StatRow = new El({ block: 'UserStatistics', elem: 'StatRow' });
    PO.UserStatistics.StatRowFirstRow = PO.UserStatistics.StatRow.nthChild(2);
    PO.UserStatistics.StatRowFirstRow.link = PO.link;
    PO.UserStatistics.StatRowFirstRow.Identifier = new El({ block: 'Identifier' });
    PO.UserStatistics.StatRowFirstRow.Identifier.link = new El('a');
    PO.UserStatistics.StatRowFirstRow.YourAnswer = new El({ block: 'UserStatistics', elem: 'YourAnswer' });
    PO.UserStatistics.StatRowFirstRow.Control = new El({ block: 'TutorTable', elem: 'Control' });
    PO.UserStatistics.StatRowSecondRow = PO.UserStatistics.StatRow.nthChild(4);
    PO.UserStatistics.StatRowSecondRow.Identifier = new El({ block: 'Identifier' });
    PO.UserStatistics.StatRowSecondRow.YourAnswer = new El({ block: 'UserStatistics', elem: 'YourAnswer' });
    PO.UserStatistics.StatRowSecondRow.Control = new El({ block: 'TutorTable', elem: 'Control' });

    PO.Filters = new El({ block: 'Filters' });
    PO.Filters.SpinnerContainer = new El({ block: 'Filters', elem: 'SpinnerContainer' });

    PO.TutorTable = new El({ block: 'TutorTable' });
    PO.TutorTable.Others = new El({ block: 'TutorTable', elem: 'Others' });
    PO.TutorTable.Row = new El({ block: 'TutorTable', elem: 'Row' });

    PO.ReportTasks = new El({ block: 'ReportTasks' });
    PO.ReportTasks.Checkbox = new El({ block: 'ReportTasks', elem: 'Checkbox' });

    PO.ReportWizardPage = new El({ block: 'ReportWizardPage' });
    PO.ReportWizardPage.ReportTasks = new El({ block: 'ReportTasks' });
    PO.ReportWizardPage.TasksToSolveList = PO.TasksToSolveList.copy();

    PO.ReportTable = new El({ block: 'ReportTable' });
    PO.ReportTable.FirstTaskLink = new El('tr').firstOfType().descendant(new El('a'));
    PO.ReportTable.LastTaskLink = new El('tr').lastOfType().descendant(new El('a'));

    PO.TagPageNavCardContainer = new El({ block: 'TagPage', elem: 'NavCardContainer' });
    PO.TagPageNavCardContainer.FirstLink = PO.link.firstOfType();
    PO.TagPageNavCardContainer.LastLink = PO.link.lastOfType();

    PO.NavCard = new El({ block: 'NavCard' });
    PO.NavCard.FirstLink = PO.link.firstOfType();
    PO.NavCard.LastLink = PO.link.lastOfType();

    PO.TagTasksToSolveList = new El({ block: 'TagTasksToSolveList' });
    PO.TagTasksToSolveListTitleLast = new El({ block: 'TagTasksToSolveList', elem: 'Title' }).lastOfType();

    PO.TagTasksToSolveList.TitleFirst = new El({ block: 'TagTasksToSolveList', elem: 'Col' }).firstOfType().descendant(new El({ block: 'Text' }));
    PO.TagTasksToSolveList.TitleLast = new El({ block: 'TagTasksToSolveList', elem: 'Col' }).lastOfType().descendant(new El({ block: 'Text' }));

    PO.FiltersAuthorSelect = new El({ block: 'Filters', elem: 'Item', modName: 'type', modVal: 'author' });
    PO.FiltersAuthorPopup = new El({ block: 'Filters', elem: 'ItemMenu', modName: 'type', modVal: 'author' });
    PO.FiltersAuthorPopup.hovered = new El({ block: 'menu__item', modName: 'hovered', modVal: 'yes' });
    PO.FiltersAuthorPopup.MenuSecond = new El({ block: 'menu__item' }).nthChild(2);
    PO.FiltersAuthorPopup.LastOption = new El({ block: 'menu__item' }).lastOfType();

    PO.FiltersYearSelect = new El({ block: 'Filters', elem: 'Item', modName: 'type', modVal: 'year' });
    PO.FiltersYearPopup = new El({ block: 'Filters', elem: 'ItemMenu', modName: 'type', modVal: 'year' });
    PO.FiltersYearPopup.hovered = new El({ block: 'control', modName: 'hovered', modVal: 'yes' });
    PO.FiltersYearPopup.LastOption = new El({ block: 'control' }).lastOfType();

    PO.Navigation = new El({ block: 'Navigation' });
    PO.NavigationCurrent = new El({ block: 'Navigation', elem: 'Current' });

    PO.TheoryContent = new El({ block: 'TheoryContent' });
    PO.TheoryContent.Title = new El({ block: 'Title' });
    PO.PrintContent = new El({ block: 'PrintContent' });
    PO.PrintTask = new El({ block: 'Task', modName: 'print', modVal: true });
    PO.PrintContent.FirstTask = PO.PrintTask.copy().nthChild(2);
    PO.PrintContent.SecondTask = PO.PrintTask.copy().nthChild(3);
    PO.PrintContent.Answers = new El({ block: 'PrintContent', elem: 'Answer' });
    PO.PrintContent.Title = new El({ block: 'Title' });
    PO.PrintTask.Answer = new El({ block: 'Task', elem: 'Answer' });
    PO.PrintTask.Solution = new El({ block: 'Task', elem: 'Solution' });
    PO.PrintTask.Author = new El({ block: 'Task', elem: 'Author' });
    PO.PrintTask.HeaderTitle = new El({ block: 'Task', elem: 'HeaderTitle' });
    PO.PrintTask.HeaderTitle.Id = PO.Identifier.copy();
    PO.PrintFilter = new El({ block: 'PrintFilter' });
    PO.PrintFilter.Container = new El({ block: 'PrintFilter', elem: 'Content' });
    PO.PrintFilter.Container.first = new El({ block: 'PrintFilter', elem: 'Item' })
        .nthChild(1)
        .descendant(PO.checkbox.label);
    PO.PrintFilter.Container.second = new El({ block: 'PrintFilter', elem: 'Item' })
        .nthChild(2)
        .descendant(PO.checkbox.label);
    PO.PrintFilter.Container.third = new El({ block: 'PrintFilter', elem: 'Item' })
        .nthChild(3)
        .descendant(PO.checkbox.label);
    PO.PrintFilter.Container.fourth = new El({ block: 'PrintFilter', elem: 'Item' })
        .nthChild(4)
        .descendant(PO.checkbox.label);

    PO.Header.RightBlock = new El({ block: 'Header', elem: 'Right' });

    PO.PrintControls = new El({ block: 'PrintControls' });
    PO.PrintControls.SelectPdfFilter = new El({ block: 'SelectPdfFilter' });
    PO.SelectPdfFilterControlMenu = new El({ block: 'SelectPdfFilter', elem: 'ControlMenu' });
    PO.SelectPdfFilterControlMenu.Tasks = new El('div').nthChild(1);
    PO.SelectPdfFilterControlMenu.Solutions = new El('div').nthChild(2);
    PO.SelectPdfFilterControlMenu.Answers = new El('div').nthChild(3);
    PO.SelectPdfFilterControlMenu.Materials = new El('div').nthChild(4);
    PO.PrintPdfToolbar = new El({ block: 'PrintPdfToolbar' });
    PO.PrintPdfToolbar.OrientationSelect = new El({ block: 'PrintPdfToolbar', elem: 'SelectControl' });
    PO.PrintPdfToolbar.Download = new El({ block: 'PrintPdfToolbar', elem: 'Download' });

    // Используем PrintPdfToolbarControlMenu в пользу PrintPdfToolbar.ControlMenu
    // Так как в DOM этот попап выносится из PrintPdfToolbar
    PO.PrintPdfToolbarControlMenu = new El({ block: 'PrintPdfToolbar', elem: 'ControlMenu' });
    PO.PrintPdfToolbarControlMenu.Landscape = new El('div').nthChild(1);
    PO.PrintPdfToolbarControlMenu.Portrait = new El('div').nthChild(2);
    PO.PrintPdfToolbarControlMenu.CopyPage = new El('div').nthChild(3);

    PO.TabsItemLast = new El({ block: 'Tabs', elem: 'Item' }).lastOfType().descendant(new El('button'));

    PO.AdminPanel = new El({ block: 'AdminPanel' });

    PO.VariantTest = new El({ block: 'VariantTest' });
    PO.VariantTest.Content = new El({ block: 'VariantTest', elem: 'Content' });
    PO.VariantTest.BlurContent = new El({ block: 'VariantTest', elem: 'Content' }).mods({ blur: true });
    PO.FinishModal = new El({ block: 'VariantTest', elem: 'FinishModal' });
    PO.FinishModal.Content = new El({ block: 'Card' });
    PO.FinishModal.Text = new El({ block: 'VariantTest', elem: 'ModalText' });
    PO.FinishModal.Accept = new El({ block: 'VariantTest', elem: 'Button', modName: 'type', modVal: 'accept' });
    PO.FinishModal.Decline = new El({ block: 'VariantTest', elem: 'Button', modName: 'type', modVal: 'decline' });

    PO.LeaveModal = new El({ block: 'LeaveModal' });
    PO.LeaveModal.Content = new El({ block: 'Card' });
    PO.LeaveModal.Text = new El({ block: 'Dialog', elem: 'Text' });
    PO.LeaveModal.Accept = new El({ block: 'Dialog', elem: 'Button', modName: 'type', modVal: 'accept' });
    PO.LeaveModal.Decline = new El({ block: 'Dialog', elem: 'Button', modName: 'type', modVal: 'decline' });

    PO.CaptchaModal = new El({ block: 'CaptchaModal' });
    PO.CaptchaModal.Form = new El({ block: 'CaptchaModal', elem: 'Form' });
    PO.CaptchaModal.inner = new El({ block: 'Modal-Content' });
    PO.CaptchaModal.input = new El('input');
    PO.CaptchaModal.Button = PO.button;

    PO.BreadCrumbs = new El({ block: 'BreadCrumbs' });
    PO.BreadCrumbs.Item = new El({ block: 'BreadCrumbs', elem: 'Item' });
    PO.BreadCrumbs.ItemLink = new El({ block: 'BreadCrumbs', elem: 'ItemLink' });

    PO.BreadCrumbs.first = PO.BreadCrumbs.Item.nthChild(1).descendant(PO.BreadCrumbs.ItemLink.copy());
    PO.BreadCrumbs.second = PO.BreadCrumbs.Item.nthChild(2).descendant(PO.BreadCrumbs.ItemLink.copy());
    PO.BreadCrumbs.third = PO.BreadCrumbs.Item.nthChild(3).descendant(PO.BreadCrumbs.ItemLink.copy());
    PO.BreadCrumbs.last = PO.BreadCrumbs.Item.lastOfType().descendant(PO.BreadCrumbs.ItemLink.copy());
    PO.BreadCrumbs.last.ItemText = new El({ block: 'BreadCrumbs', elem: 'ItemText' });

    PO.NotFoundReportLink = new El({ block: 'UpperCard' }).descendant(PO.link.copy());

    PO.InfoLine = new El({ block: 'InfoLine' });
    PO.InfoLine.RepeatButton = new El({ block: 'InfoLine', elem: 'RepeatButton' });

    PO.PrototypesLink = new El({ block: 'Task', elem: 'PrototypesLink' });
    PO.PrototypesLinkItem = PO.PrototypesLink.descendant(new El({ block: 'Text' }));

    PO.PrototypesItem = new El({ block: 'SingleTask', elem: 'PrototypesItem' });
    PO.PrototypesItem.FirstTask = new El({ block: 'Task' });

    PO.SingleTaskPage = new El({ block: 'SingleTaskPage' });

    PO.UserAttemptCol = new El({ block: 'UserAttempt', elem: 'Col' });
    PO.UserAttemptCol.Text = new El({ block: 'Text' });

    PO.StatGradSpecialVariants = new El({ block: 'StatGradSpecialVariants' });

    PO.AchievementList = new El({ block: 'AchievementList' });
    PO.AchievementList.ItemLast = new El({ block: 'AchievementList', elem: 'Item' }).lastOfType();

    PO.AchievementCard = new El({ block: 'AchievementCard' });
    PO.AchievementCard.Icon = new El({ block: 'AchievementCard', elem: 'Icon' });
    PO.AchievementCard.Link = PO.link.copy();

    PO.Achievement = new El({ block: 'Achievement' });
    PO.AchievementPopup = new El({ block: 'Achievement', elem: 'Popup' });
    PO.AchievementPopupLast = new El({ block: 'Achievement', elem: 'Popup' }).lastOfType();

    PO.AchievementShareButton = new El({ block: 'Achievement', elem: 'ShareButton' });
    PO.AchievementSharePopup = new El({ block: 'Achievement', elem: 'Popup', modName: 'type', modVal: 'share' });

    PO.CabinetRightCard = new El({ block: 'CabinetRightCard' });
    PO.CabinetRightCard.ItemText = new El({ block: 'CabinetRightCard', elem: 'ItemText' });
    PO.CabinetRightCard.StatisticLink = PO.CabinetRightCard.ItemText.mods({ name: 'statistic' }).descendant(PO.link);
    PO.CabinetRightCard.MyVariantsLink = PO.CabinetRightCard.ItemText.mods({ name: 'myVariants' }).descendant(PO.link);

    PO.MyVariant = new El({ block: 'MyVariant' });
    PO.MyVariant.ToSolution = new El({ block: 'MyVariant', elem: 'ToSolution' });
    PO.MyVariant.ToEditor = new El({ block: 'MyVariant', elem: 'ToEditor' });
    PO.MyVariant.More = new El({ block: 'MyVariant', elem: 'MoreButton' });

    PO.MyVariantPopup = new El({ block: 'MyVariant', elem: 'Popup' });
    PO.MyVariantPopup.PrintAction = new El({ block: 'MyVariant', elem: 'PrintAction' });
    PO.MyVariantPopup.CopyAction = new El({ block: 'MyVariant', elem: 'CopyAction' });
    PO.MyVariantPopup.DeleteAction = new El({ block: 'MyVariant', elem: 'DeleteAction' });

    PO.DeleteVariantModal = new El({ block: 'DeleteVariantModal' });
    PO.DeleteVariantModal.ActionButton = new El({ block: 'DeleteVariantModal', elem: 'ActionButton' });

    PO.MyVariantsList = new El({ block: 'MyVariantsList' });
    PO.MyVariantsList.CreateButton = new El({ block: 'MyVariantsList', elem: 'CreateNewVariant' });
    PO.MyVariantsList.Row = new El('').child(PO.Row);
    PO.MyVariantsList.FirstRow = new El('').child(PO.Row).nthType(2);
    PO.MyVariantsList.FirstRow.MyVariant = PO.MyVariant.copy();

    PO.NeedLogin = new El({ block: 'NeedLogin' });
    PO.NeedLogin.Link = new El({ block: 'NeedLogin', elem: 'Link' });

    PO.RecommendedTasks = new El({ block: 'RecommendedTasks' });
    PO.RecommendedTasks.Button = new El({ block: 'RecommendedTasks', elem: 'Button' });
    PO.RecommendedTasks.EmptyButton = new El({ block: 'RecommendedTasks', elem: 'EmptyButton' });
    PO.RecommendedTasks.Task = new El({ block: 'RecommendedTasks', elem: 'CollectionCurrentTask' }).descendant(PO.Task.copy());
    PO.RecommendedTasks.TaskControlLine = PO.TaskControlLine.copy();
    PO.RecommendedTasks.UserAttemptCol = PO.UserAttemptCol.copy();
    PO.RecommendedTasks.ImmediateResult = new El({ block: 'RecommendedTasks', elem: 'ImmediateResult' });
    PO.RecommendedTasks.Result = new El({ block: 'RecommendedTasks', elem: 'Result' });

    PO.MaterialsPage = new El({ block: 'MaterialsPage' });
    PO.MaterialsPage.Results = new El({ block: 'Materials', elem: 'Results' });
    PO.MaterialsPage.Content = new El({ block: 'Materials', elem: 'Content' });
    PO.MaterialsPage.ContentLoading = new El({ block: 'Materials', elem: 'Content' }).mods({ loading: true });
    PO.SearchForm = new El({ block: 'SearchForm' });
    PO.SearchForm.Input = new El({ block: 'SearchForm', elem: 'Input' });
    PO.SearchForm.Input.Control = new El({ block: 'textinput__control' }); // старый контрол из lor
    PO.SearchForm.Submit = new El({ block: 'SearchForm', elem: 'Lense' });
    PO.PopularQueries = new El({ block: 'PopularQueries' });
    PO.Suggest = new El({ block: 'SearchForm', elem: 'Popup' });

    PO.Problems = new El({ block: 'Problems' });
    PO.ProblemsLoading = new El({ block: 'Problems' }).mods({ loading: true });
    PO.Problems.Snippet = new El({ block: 'Problems', elem: 'Item' });
    PO.Problems.Snippet.first = new El({ block: 'ProblemSnippet' }).firstOfType();

    PO.Promo = new El({ block: 'Promo' });
    PO.Promo.Controls = new El({ block: 'Promo', elem: 'Controls' });
    PO.Promo.Controls.Btn = PO.button;
    PO.Promo.LinkContainer = new El({ block: 'Promo', elem: 'LinkContainer' });

    PO.PromoFeedback = PO.Promo.copy().mix(new El({ block: 'PromoFeedback' }));
    PO.PromoTaskOfTheDay = PO.Promo.copy().mix(new El({ block: 'PromoTaskOfTheDay' }));
    PO.PromoHardestTask = PO.Promo.copy().mix(new El({ block: 'PromoHardestTask' }));

    PO.GrabMover = new El({ block: 'GrabMover' });
    PO.GrabMover.Card = new El({ block: 'Card' });
    PO.GrabMover.Content = new El({ block: 'BlocksMoving' });
    PO.GrabMover.Content.FirstItem = new El({ block: 'BlocksMoving', elem: 'Item' }).nthType(1);
    PO.GrabMover.Content.SecondItem = new El({ block: 'BlocksMoving', elem: 'Item' }).nthType(2);
    PO.GrabMover.Content.FouthItem = new El({ block: 'BlocksMoving', elem: 'Item' }).nthType(4);

    PO.TaskTitles = PO.PageLayout.Left.copy()
        .descendant(new El({ block: 'BlocksAnimateMoving' }))
        .descendant(new El({ block: 'BlocksAnimateMoving', elem: 'Item' }))
        .descendant(PO.Identifier.copy());

    PO.ShareLandingPopup = new El({ block: 'SharingLandingModal' });
    PO.ShareLandingPopup.Button = new El({ block: 'SharingLandingModal', elem: 'Button' });

    PO.ManualAd = new El({ block: 'ManualAd' });

    return PO;
};
