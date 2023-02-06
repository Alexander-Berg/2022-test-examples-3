import { Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import React from "react";

const history = createMemoryHistory();

export const TestBed = ({ children }) => (
    <Router history={history}>{children}</Router>
);
