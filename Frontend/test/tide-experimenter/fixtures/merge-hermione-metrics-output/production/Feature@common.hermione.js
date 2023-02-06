specs({ feature: 'Feature-name' }, () => {
    /* <<<<<<< production */
    it('First it', () => {
        console.log();
    });
    /*
    =======
    */
    it('First it', () => {});/*
    >>>>>>> experiment */
    it('Third it', () => {});
    it('Second it', () => {});
});
