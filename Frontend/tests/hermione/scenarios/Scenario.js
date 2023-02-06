module.exports = class Scenario {
    triggers = [];
    steps = [];

    constructor(
        name = 'Название по умолчанию',
        icon = 'home',
    ) {
        this.name = name;
        this.icon = icon;
    }

    addTrigger(value) {
        this.triggers.push(value);
    }

    removeTriggers() {
        this.triggers = [];
    }

    addStep(value) {
        this.steps.push(value);
    }

    getInfo() {
        return {
            name: this.name,
            icon: this.icon,
            triggers: this.triggers,
            steps: this.steps,
        };
    }
};
