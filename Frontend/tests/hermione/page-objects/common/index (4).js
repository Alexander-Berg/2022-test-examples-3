const El = require('../Entity');

const elems = {};

elems.Link = new El({ block: 'Link' });
elems.Button = new El({ block: 'Button' });
elems.Suggest = new El({ block: 'search-suggest' });

elems.Header = new El({ block: 'Header' });
elems.Header.Form = new El('form');
elems.Header.Input = new El('[name="text"]');
elems.Header.ClearBtn = new El('[class*="header__clear"]');
elems.Header.Suggest = elems.Suggest;
elems.Header.Suggest.Input = new El({ block: 'search-suggest__input' });
elems.Header.Suggest.InputClear = new El({ block: 'search-suggest__input-clear' });
elems.Header.Suggest.SearchButton = new El({ block: 'search-suggest__button' });
elems.Header.Suggest.Popup = new El({ block: 'search-suggest__popup' });
elems.Header.Suggest.FirstItem = new El({ block: 'search-suggest__item' }).firstChild();

elems.HeaderTabs = new El({ block: 'HeaderTabs' });

elems.Layout = new El({ block: 'MainLayout' });
elems.Layout.Header = new El({ block: 'MainLayout-Header' });
elems.Layout.HeaderTabs = new El({ block: 'MainLayout-HeaderTabs' });

elems.PillCategoryItemsFetchMoreRow = new El({ block: 'PillCategoryItems-FetchMoreRow' });
elems.PillCategoryItemsFetchMoreRow.button = elems.Button.copy();

elems.PillCategoryItemsPageTitle = new El({ block: 'PillsCategoryItemsPage', elem: 'Title' });
elems.PillsCategoryItemsPageItems = new El({ block: 'PillsCategoryItemsPage', elem: 'Items' });
elems.PillShowMoreButton = new El({ block: 'PillCategoryItems', elem: 'FetchMoreRow' });
elems.MainContent = new El({ block: 'MainContentContainer' });
elems.PillCard = new El({ block: 'PillCard' });
elems.PillCardBoardItem = new El({ block: 'PillCardBoard', elem: 'Item' });

elems.Modal = new El({ block: 'Modal' });
elems.ModalOverlay = new El({ block: 'Modal-Overlay' });
elems.Modal.CloseButton = new El({ block: 'CloseButton' });
elems.ModalLayoutContentLast = new El({ block: 'ModalLayoutContent' }).lastChild();

elems.PillsPrices = new El({ block: 'PillsPrices' });
elems.PillsPrices.Link = elems.Link.copy();

elems.PillTitle = new El({ block: 'Title' });
elems.PillSubtitle = new El({ block: 'Title-Subtitle' });
elems.PillSummary = new El({ block: 'PillSummary' });

elems.TelemedAskForm = new El({ block: 'TelemedAskForm' });
elems.TelemedAskForm.Button = new El({ block: 'TelemedAskForm', elem: 'Button' });
elems.TelemedPrices = new El({ block: 'TelemedPrices' });
elems.TelemedPrices.Content = new El({ block: '.Tray-Content' });

elems.PillCategoriesCellLink = new El({ block: 'PillCategories-CellLink' });

elems.PillCategoriesRow = new El({ block: 'PillCategories-Row' });
elems.PillCategoriesRowCell = new El({ block: 'PillCategories-Cell' });
elems.PillCategoriesRowCellSecond = new El({ block: 'PillCategories-Cell' }).nthChild(2);

elems.PillCategories = new El({ block: 'PillCategoriesPage-Categories' });
elems.PillCategoriesCategoryHeader = new El({ block: 'PillCategories-CategoryHeaderLink' });
elems.PillCategoriesCategoryHeaderSecond = new El({ block: 'PillCategories-CategoryHeaderLink' }).nthChild(3);

elems.PillCategoriesSubcategoryBoard = new El({ block: 'PillCategories-SubcategoryBoard' });
elems.PillCategoriesSubcategoryBoard.SubcategoryItem = new El({ block: 'PillCategories-SubcategoryItem' });

elems.PillCategoriesCategoryHeaderClick = new El({ block: 'PillCategories-CategoryHeaderLink' });
elems.PillCategoriesCategoryChildList = new El({ block: 'PillCategories-CategoryChildList' });
elems.PillCategoriesCategoryChildList.SubcategoryItem = new El({ block: 'PillCategories-CategoryChildItem' });

elems.PillsArticles = new El({ block: 'PillArticlesBlock' });
elems.PillsArticles.FirstArticle = new El({ block: 'ArticleCard' }).firstChild();
elems.PillsArticles.FirstArticle.HeaderLink = new El({ block: 'ArticleCard-Title' });

elems.PillTopBanner = new El({ block: 'TopBanner' });
elems.PillSideBanner = new El({ block: 'SideBanner' });
elems.PillBottomBanner = new El({ block: 'BottomBanner' });
elems.PillAdBlockThemeHeader = new El({ block: 'AdBlock_theme_Header' });
elems.PillAdBlockThemeContentFirst = new El({ block: 'AdBlock_theme_Content:nth-of-type(6)' });
elems.PillAdBlockThemeContentSecond = new El({ block: 'AdBlock_theme_Content:nth-of-type(12)' });
elems.PillAdBlockThemeContentThird = new El({ block: 'AdBlock_theme_Content:nth-of-type(17)' });
elems.AdBlockThemeFooter = new El({ block: 'AdBlock_theme_Footer' });

elems.Footer = new El({ block: 'Footer' });

elems.BannerTelemed = new El({ block: 'BannerTelemed' });
elems.BannerTelemed.CloseButton = new El({ block: 'CloseButton' });

elems.QCard = new El({ block: 'QCard' }).firstChild();
elems.QCardButton = new El({ block: 'QCard', elem: 'Button' }).firstChild();

elems.PillProductPage = new El({ block: 'PillProductPage' });

elems.PillSummary.Content = new El({ block: 'PillSummary', elem: 'Content' });
elems.PillSummary.OrderPanel = new El({ block: 'PillOrderPanel' });
elems.PillSummary.OrderPanel.PriceLink = new El({ block: 'Link' });
elems.PillSummary.OrderPanel.OrderButton = new El({ block: 'Button' });
elems.PillSummary.PackingSlider = new El({ block: 'PillPackingSlider' });
elems.PillSummary.PackingSlider.FirstPacking = new El({ block: 'PillPacking' }).firstChild();
elems.PillSummary.PillPackingSliderMore = new El({ block: 'PillPackingSlider', elem: 'More' });
elems.PillSummary.PillPackingSliderExpandedArea = new El({ block: 'PillPackingSlider', elem: 'ExpandedArea' });
elems.PillSummary.DisclaimersList = new El({ block: 'PillDisclaimersList' });
elems.PillSummary.DisclaimersList.FirstDisclaimer = new El({ block: 'PillImageDisclaimer' }).firstChild();

elems.ShortReleaseForms = new El({ block: 'PillShortReleaseForms' });
elems.ShortReleaseForms.FirstForm = new El({ block: 'HorizontalScroll', elem: 'Item' }).firstChild();
elems.ShortReleaseForms.FirstForm.Link = new El({ block: 'Link' });

elems.PillInstruction = new El({ block: 'PillInstruction' });
elems.PillInstruction.Navigation = new El({ block: 'PillInstruction', elem: 'Navigation' });
elems.PillInstruction.Navigation.FirstItem = new El({ block: 'PillInstruction', elem: 'NavigationItem' }).firstChild();
elems.PillInstruction.InfoPillAnchor = new El({ block: 'PillInstruction', elem: 'Info' }).mods({ type: 'pillAnchor' });
elems.PillInstruction.InfoPillAnchor.Link = new El({ block: 'PillInstruction', elem: 'Link' });
elems.PillInstruction.Accordion = new El({ block: 'Accordion' });
elems.PillInstruction.Accordion.FirstItem = new El({ block: 'Accordion', elem: 'Item' }).firstChild();
elems.PillInstruction.Accordion.FirstItem.Button = new El({ block: 'Button' });

elems.DrugList = new El({ block: 'DrugList' });
elems.DrugList.ItemList = new El({ block: 'DrugList', elem: 'ItemList' });
elems.DrugList.ItemList.Item = new El({ block: 'Drug' });
elems.DrugList.ItemList.FirstItem = new El({ block: 'Drug' }).firstChild();
elems.DrugList.Footer = new El({ block: 'DrugList', elem: 'Footer' });
elems.DrugList.Footer.MoreButton = new El({ block: 'Button' });

elems.PillArticleList = new El({ block: 'PillArticleList' });
elems.PillArticleList.ArticleMain = new El({ block: 'ArticleCard', elem: 'Main' });
elems.PillArticleList.FirstArticle = new El({ block: 'ArticleCard' }).firstChild();
elems.PillArticleList.FirstArticle.HeaderLink = new El({ block: 'ArticleCard-Title' });

elems.DrugFilterList = new El({ block: 'DrugFilterList' });
elems.DrugFilterList.CommonFilter = new El({ block: 'DrugFilterList', elem: 'Wrapper' }).firstChild();
elems.DrugFilterList.CountryFilter = new El({ block: 'DrugFilterList', elem: 'Wrapper' }).nthChild(2);
elems.DrugFilterList.ActiveFilter = new El({ block: 'DrugFilterList', elem: 'Item' }).mods({ active: true });
elems.DrugFilterList.ActiveFilter.CloseIcon = new El({ block: 'DrugFilterList', elem: 'ItemIcon' });

elems.Select = new El({ block: 'Select', elem: 'Popup' });
elems.Select.Content = new El({ block: 'Select', elem: 'Options' });
elems.Select.Content.firstItem = new El({ block: 'Select', elem: 'Option' }).firstChild();
elems.Select.Content.secondItem = new El({ block: 'Select', elem: 'Option' }).nthChild(2);
elems.SelectCloseSpace = new El({ block: 'DrugList' });

// storybook
elems.ScreenshotContainer = new El({ block: 'ScreenshotContainer' });

elems.PillOrderPanel = new El({ block: 'PillOrderPanel' });
elems.PillOrderPanel.Button = new El({ block: 'Button' });

elems.CartBadge = new El({ block: 'CartBadge' });
elems.Cart = new El({ block: 'Cart' });
elems.CartFooter = new El({ block: 'Cart', elem: 'Footer' });
elems.CartCounter = new El({ block: 'CartCounter' });
elems.CartCounter.PlusButton = new El({ block: 'Button' }).nthChild(3);

elems.SubstanceSummary = new El({ block: 'SubstanceSummary' });

// В тестах нужен открытый. Треев может быть много, а открытый - один
elems.Tray = new El({ block: 'Tray' }).mods({ mode: 'open' });
elems.Tray.TrayClose = new El({ block: 'Tray', elem: 'Close' });

elems.NotFoundPage = new El({ block: 'NotFoundRoot' });

module.exports = elems;
