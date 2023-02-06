importPackage(ru.yandex.chemodan.app.lentaloader.cool.utils);
importPackage(ru.yandex.chemodan.app.lentaloader.cool.model);
var block = coolLentaManager.getAllBlocks(new ru.yandex.inside.passport.PassportUid(50273844)).filter(function(block) {return "thematic_month_1149105600000_mountains" === block.getId()}).first();
var blockInterval = block.getGenerationInterval();
var context = new TitleGenerationContext(new Random2(block.getBestResourceId.hashCode()), blockInterval.type, blockInterval.start).withTerm(themeDefinitionRegistry.getO("mountains").get().getForms()).withAttribute("theme_id", "mountains");
blockTitilesGenerator.generateLegacyBlockTitle(context);
tankerTextGenerator.processOneFromSetOrTemplatesByKeyPrefix("lenta/thematic_blocks_text", context);