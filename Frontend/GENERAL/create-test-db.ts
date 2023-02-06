/* eslint-disable */
import { resolve } from 'path';

import { createImage, createSkill, createUser } from '../test/functional/_helpers';
import { ImageType } from '../db/tables/image';

process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.CFG_DIR = resolve(__dirname, '../configs');

(async() => {
    await createUser();
    const skill1 = await createSkill({
        name: 'города',
        activationPhrases: ['города'],
        slug: 'slug-1',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'игра в города', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'города', request: '' },
                { marker: 'сыграем в', activationPhrase: 'города', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill2 = await createSkill({
        name: 'приключение линка',
        activationPhrases: ['приключение линка'],
        slug: 'slug-2',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'приключение линка', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'приключение линка', request: '' },
                { marker: 'сыграем в', activationPhrase: 'приключение линка', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill3 = await createSkill({
        name: 'приключение зельды',
        activationPhrases: ['приключение зельды'],
        slug: 'slug-3',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'приключение зельды', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'приключение зельды', request: '' },
                { marker: 'сыграем в', activationPhrase: 'приключение зельды', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill4 = await createSkill({
        name: 'квест',
        activationPhrases: ['квест'],
        slug: 'slug-4',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'квест', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'квест', request: '' },
                { marker: 'сыграем в', activationPhrase: 'квест', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill5 = await createSkill({
        name: 'мекка инструмента',
        activationPhrases: ['мекка инструмента'],
        slug: 'slug-5',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'мекка инструмента', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'мекка инструмента', request: '' },
                { marker: 'сыграем в', activationPhrase: 'мекка инструмента', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill6 = await createSkill({
        name: 'компьютерный мастер',
        activationPhrases: ['компьютерный мастер'],
        slug: 'slug-6',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'компьютерный мастер', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'компьютерный мастер', request: '' },
                { marker: 'сыграем в', activationPhrase: 'компьютерный мастер', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill7 = await createSkill({
        name: 'Игра математика',
        activationPhrases: ['Игра математика'],
        slug: 'slug-7',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'Игра математика', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'Игра математика', request: '' },
                { marker: 'сыграем в', activationPhrase: 'Игра математика', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill8 = await createSkill({
        name: 'Игра миллионер',
        activationPhrases: ['Игра миллионер'],
        slug: 'slug-8',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'Игра миллионер', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'Игра миллионер', request: '' },
                { marker: 'сыграем в', activationPhrase: 'Игра миллионер', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill9 = await createSkill({
        name: 'домик в деревне',
        activationPhrases: ['домик в деревне'],
        slug: 'slug-9',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'food_drink',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'игра в домик в деревне', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'домик в деревне', request: '' },
                { marker: 'сыграем в', activationPhrase: 'домик в деревне', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill10 = await createSkill({
        name: 'компьютерный мастер',
        activationPhrases: ['компьютерный мастер'],
        slug: 'slug-10',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'food_drink',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'компьютерный мастер', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'компьютерный мастер', request: '' },
                { marker: 'сыграем в', activationPhrase: 'компьютерный мастер', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill11 = await createSkill({
        name: 'тест тест тест',
        activationPhrases: ['тест тест тест'],
        slug: 'slug-11',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'communication',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'Игра математика', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'Игра математика', request: '' },
                { marker: 'сыграем в', activationPhrase: 'Игра математика', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill12 = await createSkill({
        name: 'Городские легенды',
        activationPhrases: ['Городские легенды'],
        slug: 'slug-12',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'communication',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'Городские легенды', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'Городские легенды', request: '' },
                { marker: 'сыграем в', activationPhrase: 'Городские легенды', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });
    const skill13 = await createSkill({
        name: 'медуза новости',
        activationPhrases: ['медуза новости'],
        slug: 'slug-13',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'медуза новости', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'медуза новости', request: '' },
                { marker: 'сыграем в', activationPhrase: 'медуза новости', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill14 = await createSkill({
        name: 'уличные гонки',
        activationPhrases: ['уличные гонки'],
        slug: 'slug-14',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'games_trivia_accessories',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'уличные гонки', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'уличные гонки', request: '' },
                { marker: 'сыграем в', activationPhrase: 'уличные гонки', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill15 = await createSkill({
        name: 'городской модник',
        activationPhrases: ['городской модник'],
        slug: 'slug-15',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'health_fitness',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'городской модник', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'городской модник', request: '' },
                { marker: 'сыграем в', activationPhrase: 'городской модник', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill16 = await createSkill({
        name: 'мечта миллионера',
        activationPhrases: ['мечта миллионера'],
        slug: 'slug-16',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'health_fitness',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'мечта миллионера', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'мечта миллионера', request: '' },
                { marker: 'сыграем в', activationPhrase: 'мечта миллионера', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill17 = await createSkill({
        name: 'сто к одному',
        activationPhrases: ['сто к одному'],
        slug: 'slug-17',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'health_fitness',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'сто к одному', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'сто к одному', request: '' },
                { marker: 'сыграем в', activationPhrase: 'сто к одному', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill18 = await createSkill({
        name: 'пещерный квест',
        activationPhrases: ['пещерный квест'],
        slug: 'slug-18',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'lifestyle',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'игра в домик в деревне', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'домик в деревне', request: '' },
                { marker: 'сыграем в', activationPhrase: 'домик в деревне', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill19 = await createSkill({
        name: 'мир приключений',
        activationPhrases: ['мир приключений'],
        slug: 'slug-19',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'lifestyle',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'мир приключений', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'мир приключений', request: '' },
                { marker: 'сыграем в', activationPhrase: 'мир приключений', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const skill20 = await createSkill({
        name: 'кот василий',
        activationPhrases: ['кот василий'],
        slug: 'slug-20',
        onAir: true,
        publishingSettings: {
            brandVerificationWebsite: '',
            category: 'kids',
            developerName: 'Яндекс',
            explicitContent: false,
            structuredExamples: [
                { marker: 'запусти навык', activationPhrase: 'кот василий', request: '' },
                { marker: 'давай поиграем в', activationPhrase: 'кот василий', request: '' },
                { marker: 'сыграем в', activationPhrase: 'кот василий', request: '' },
            ],
            description: 'Вы мне — Самара, а я вам — Архангельск',
            email: '',
            emailIsVerified: false,
            showBrandVerificationWebsiteInStore: true,
        },
    });

    const image1 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image2 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image3 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image4 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image5 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image6 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image7 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image8 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image9 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image10 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image11 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image12 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image13 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image14 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image15 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image16 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image17 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image18 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image19 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image20 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });

    await skill1.update({ logoId: image1.id });
    await skill2.update({ logoId: image2.id });
    await skill3.update({ logoId: image3.id });
    await skill4.update({ logoId: image4.id });
    await skill5.update({ logoId: image5.id });
    await skill6.update({ logoId: image6.id });
    await skill7.update({ logoId: image7.id });
    await skill8.update({ logoId: image8.id });
    await skill9.update({ logoId: image9.id });
    await skill10.update({ logoId: image10.id });
    await skill11.update({ logoId: image11.id });
    await skill12.update({ logoId: image12.id });
    await skill13.update({ logoId: image13.id });
    await skill14.update({ logoId: image14.id });
    await skill15.update({ logoId: image15.id });
    await skill16.update({ logoId: image16.id });
    await skill17.update({ logoId: image17.id });
    await skill18.update({ logoId: image18.id });
    await skill19.update({ logoId: image19.id });
    await skill20.update({ logoId: image20.id });

    process.exit(0);
})().catch(err => {
    console.error(err);
    process.exit(1);
});
