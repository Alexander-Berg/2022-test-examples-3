import { Component } from 'react';

class TestExample extends Component {
    public componentWillMount() {
        throw new Error('Test example error');
    }
}

export default TestExample;
