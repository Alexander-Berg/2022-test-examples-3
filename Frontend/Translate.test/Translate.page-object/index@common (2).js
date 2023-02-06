const POorganic = require('../../../../features/Organic/Organic.test/Organic.page-objects');
const { Entity, ReactEntity } = require('../../../../vendors/hermione');

module.exports = function() {
    const elems = {};

    elems.factTranslate = new Entity({ block: 'fact-layout' });

    elems.fact = new Entity({ block: 'fact' });
    elems.fact.greenurl = POorganic.Path.copy();
    elems.fact.title = new Entity({ block: 'fact', elem: 'title' });
    elems.fact.sourceLink = new Entity({ block: 'fact', elem: 'source-link' });
    elems.fact.answer = new Entity({ block: 'fact', elem: 'answer' });
    elems.fact.answer.link = new Entity({ block: 'link' });
    elems.fact.footer = new ReactEntity({ block: 'ExtraActions', elem: 'ReportItem' });

    const block = 'Translate';

    elems.translate = new Entity({ block: 't-construct-adapter', elem: 'translate' });
    elems.translate.translate = new ReactEntity({ block });
    elems.translate.favicon = new ReactEntity({ block: 'Favicon' });
    elems.translate.title = POorganic.OrganicTitle.copy();
    elems.translate.greenurl = POorganic.Path.copy();
    elems.translate.form = new ReactEntity({ block, elem: 'FullTranslationForm' });
    elems.translate.fullLink = new ReactEntity({ block, elem: 'FullTranslationLink' });
    elems.translate.input = new ReactEntity({ block, elem: 'Input' });
    elems.translate.textArea = new ReactEntity({ block: 'Translate', elem: 'TextArea' });
    elems.translate.textArea.control = new ReactEntity({ block: 'Textarea', elem: 'Control' });
    elems.translate.result = new ReactEntity({ block, elem: 'Output' });
    elems.translate.resultText = new ReactEntity({ block, elem: 'OutputText' });
    elems.translate.resultDict = new ReactEntity({ block, elem: 'DictionaryView' });
    elems.translate.resultArticle = new ReactEntity({ block, elem: 'Article' });
    elems.translate.langControls = new ReactEntity({ block, elem: 'Controls' });
    elems.translate.swapLangsButton = new ReactEntity({ block, elem: 'ReverseButton' });
    elems.translate.sourceLangSwitch = new ReactEntity({ block, elem: 'Select' }).mods({ type: 'source' });
    elems.translate.sourceLangSwitch.button = new ReactEntity({ block: 'Select2', elem: 'Button' });
    elems.translate.targetLangSwitch = new ReactEntity({ block, elem: 'Select' }).mods({ type: 'target' });
    elems.translate.targetLangSwitch.button = new ReactEntity({ block: 'Select2', elem: 'Button' });
    elems.translate.copyIcon = new ReactEntity({ block, elem: 'Copy' });
    elems.translate.copyTooltip = new ReactEntity({ block, elem: 'CopyTooltip' });
    elems.translate.inputVoice = new ReactEntity({ block, elem: 'Voice' });
    elems.translate.speakerSource = new ReactEntity({ block: 'Translate', elem: 'Speaker' }).mods({ direction: 'source' });
    elems.translate.speakerSourceSpeaking = new ReactEntity({ block: 'Translate', elem: 'Speaker' }).mods({ direction: 'source', speaking: true });
    elems.translate.speakerTarget = new ReactEntity({ block: 'Translate', elem: 'Speaker' }).mods({ direction: 'target' });
    elems.translate.speakerTargetSpeaking = new ReactEntity({ block: 'Translate', elem: 'Speaker' }).mods({ direction: 'target', speaking: true });
    elems.translate.spinner = new ReactEntity({ block: 'Translate', elem: 'SpinWrap' });
    elems.translate.sourceTranscription = new ReactEntity({ block: 'Translate', elem: 'Transcription' }).mods({ type: 'src' });
    elems.translate.targetTranscription = new ReactEntity({ block: 'Translate', elem: 'Transcription' }).mods({ type: 'dst' });
    elems.translate.suggest = new ReactEntity({ block: 'Translate', elem: 'Suggest' });

    elems.translate.maximizeButton = new ReactEntity({ block, elem: 'Button' });
    elems.translate.maximizeButtonText = new ReactEntity({ block, elem: 'ButtonText' });
    elems.translate.examples = new ReactEntity({ block, elem: 'Examples' });
    elems.translate.examplesText = new ReactEntity({ block, elem: 'Text' });
    elems.translate.refreshButton = new ReactEntity({ block, elem: 'Refresh' });
    elems.translate.errorMessage = new ReactEntity({ block, elem: 'ErrorMessage' });
    elems.translate.srcIcon1 = new ReactEntity({ block, elem: 'SourceIcon' }).mods({ index: 1 });
    elems.translate.srcIcon2 = new ReactEntity({ block, elem: 'SourceIcon' }).mods({ index: 2 });

    return elems;
};
