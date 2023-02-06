export default interface IReport {
    pushError(error: any): void;
    getCountErrors(): number;
    errorsToTxt(): string;
    clearErrors(): void;
    pushInfo(message: string): void;
    infotoTxt(): string;
    clearInfo(): void;
}
