const El = require('../Entity');

const elems = {};

elems.YndxBug = new El({ block: 'YndxBug' });

elems.YndxBug.Bug = new El({ block: 'Bug__bug' });
elems.YndxBug.Paranja = new El({ block: 'Bug__paranja' });
elems.YndxBug.HideBtn = new El({ block: 'Bug__hide' });
elems.YndxBug.CloseBtn = new El({ block: 'Bug__close' }).nthChild(2);
elems.YndxBug.Icon = new El({ block: 'Bug__icon' });

elems.Tabs = new El({ block: 'Tabs__head' });
elems.Tabs.ScreenshotPreview = new El({ block: 'Tabs__tab' }).nthChild(2);
elems.Tabs.AB = new El({ block: 'Tabs__tab' }).nthChild(2);
elems.Tabs.Session = new El({ block: 'Tabs__tab' }).nthChild(3);
elems.Tabs.Info = new El({ block: 'Tabs__tab' }).lastChild();

elems.YaForm = new El({ block: 'YaForm__form' });
elems.FormInput = new El('.input__control');
elems.ShowButton = new El('.show-bug-button');
elems.EnableScreenshot = new El('.enable-screenshot-button');

elems.Exp = new El({ block: 'ExpList__exp' });
elems.FirstExp = new El({ block: 'ExpList__exp' }).firstChild();
elems.ThirdExp = new El({ block: 'ExpList__exp' }).nthChild(3);
elems.LastExp = new El({ block: 'ExpList__exp' }).lastChild();
elems.StuckBtn = new El('[data-action=stuck]');
elems.UnstuckBtn = new El('[data-action=unstuck]');
elems.UnstuckUrl = new El('[data-action=url-unstuck]');

elems.AB = new El({ block: 'YndxBug__AB' });
elems.AB.StuckTab = new El('.Tabs__tab').firstChild();
elems.AB.StuckPage = new El('.Tabs__block').firstChild();
elems.AB.CurExpTab = new El('.Tabs__tab').nthChild(2);
elems.AB.CurExpPage = new El('.Tabs__block').nthChild(2);
elems.AB.UnstuckAllBtn = new El('.ExpPage__header .Button__button');

elems.AB.StuckPage.Input = new El('[placeholder="Поиск эксперимента"]');

elems.AB.CurExpPage.Input = new El('[placeholder="Поиск эксперимента"]');

elems.ExpList = new El({ block: 'YndxBug__MainList' });
elems.ExpList.CleanYandexBtn = new El('button[data-test-id="1"][data-action="stuck"]');

elems.ExpStuck = new El({ block: 'ExpStuck__container' });
elems.ExpStuck.HideBtn = new El({ block: 'ExpStuck__hint' });
elems.ExpStuck.ExpList = new El({ block: 'ExpList__list' });
elems.ExpStuck.CleanYandexBtn = new El('button[data-test-id="1"]');

elems.Session = new El({ block: 'Session__session' });
elems.Session.ControlBar = new El({ block: 'Session__controlBar' });
elems.Session.StartBtn = new El({ block: 'Session__button' });
elems.Session.Input = new El('[placeholder="Название сессии"');
elems.Session.Name = new El({ block: 'Session__sessionInfo__name' });
elems.Session.Info = new El({ block: 'Session__sessionInfo__content' });
elems.Session.Info.Status = new El('> div');
elems.Session.Info.Link = new El('a');

elems.Info = new El({ block: 'Info__info' });
elems.Info.content = new El({ block: 'Info__content' });

elems.ScreenshotPreview = new El({ block: 'ScreenshotPreview__screenshotPreviewTab' });
elems.ScreenshotPreview.Screenshot = new El({ block: 'ScreenshotPreview__preview' });

module.exports = elems;
